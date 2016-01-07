package cn.slimsmart.thrift.rpc;

import cn.slimsmart.thrift.rpc.ThriftAsyncClientPoolFactory.PoolOperationCallBack;
import cn.slimsmart.thrift.rpc.zookeeper.ThriftServerAddressProvider;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.async.TAsyncClientFactory;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 客户端代理
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ThriftServiceAsyncClientProxyFactory implements FactoryBean, InitializingBean,Closeable {
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	private Integer maxActive = 32;// 最大活跃连接数

	// ms,default 3 min,链接空闲时间
	// -1,关闭空闲检测
	private Integer idleTime = 180000;
	private ThriftServerAddressProvider serverAddressProvider;

	private Object proxyClient;
	private Class<?> objectClass;

	private GenericObjectPool<TAsyncClient> pool;

	private static TAsyncClientManager clientManager = null;

	private TProtocolFactory protocol = new TBinaryProtocol.Factory();


	private PoolOperationCallBack callback = new PoolOperationCallBack() {
		@Override
		public void destroy(TAsyncClient client) {
			logger.info("destroy");
		}

		@Override
		public void make(TAsyncClient client) {
			logger.info("create");
		}
	};
	
	public void setMaxActive(Integer maxActive) {
		this.maxActive = maxActive;
	}

	public void setIdleTime(Integer idleTime) {
		this.idleTime = idleTime;
	}

	public void setServerAddressProvider(ThriftServerAddressProvider serverAddressProvider) {
		this.serverAddressProvider = serverAddressProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(clientManager == null) {
			clientManager = new TAsyncClientManager();
		}
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		// 加载Iface接口
		objectClass = classLoader.loadClass(serverAddressProvider.getService() + "$AsyncIface");
		// 加载Client.Factory类
		Class<TAsyncClientFactory<TAsyncClient>> fi = (Class<TAsyncClientFactory<TAsyncClient>>) classLoader.loadClass(serverAddressProvider.getService() + "$AsyncClient$Factory");
		TAsyncClientFactory<TAsyncClient> clientFactory = fi.getConstructor(TAsyncClientManager.class,TProtocolFactory.class).newInstance(clientManager, protocol);
		ThriftAsyncClientPoolFactory clientPool = new ThriftAsyncClientPoolFactory(serverAddressProvider, clientFactory, callback);
		GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
		poolConfig.maxActive = maxActive;
		poolConfig.maxIdle = 1;
		poolConfig.minIdle = 0;
		poolConfig.minEvictableIdleTimeMillis = idleTime;
		poolConfig.timeBetweenEvictionRunsMillis = idleTime * 2L;
		poolConfig.testOnBorrow=true;
		poolConfig.testOnReturn=false;
		poolConfig.testWhileIdle=false;
		pool = new GenericObjectPool<TAsyncClient>(clientPool, poolConfig);
		proxyClient = Proxy.newProxyInstance(classLoader, new Class[] { objectClass }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				//
				TAsyncClient client = pool.borrowObject();
				try {
					for(Object obj :args) {
						Class clazz = AsyncMethodCallback.class;
						if(clazz.isAssignableFrom(obj.getClass())) {
							Method m = obj.getClass().getMethod("setPool",GenericObjectPool.class);
							m.invoke(obj, pool);
							m = obj.getClass().getMethod("setClient",TAsyncClient.class);
							m.invoke(obj, client);
						}
					}
					return method.invoke(client, args);
				} catch (Exception e) {
					pool.returnObject(client);
					throw e;
				}
			}
		});
	}

	@Override
	public Object getObject() throws Exception {
		return proxyClient;
	}

	@Override
	public Class<?> getObjectType() {
		return objectClass;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void close() {
		if(pool!=null){
			try {
				pool.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (serverAddressProvider != null) {
			try {
				serverAddressProvider.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
