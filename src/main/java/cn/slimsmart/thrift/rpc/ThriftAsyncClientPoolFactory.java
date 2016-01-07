package cn.slimsmart.thrift.rpc;

import cn.slimsmart.thrift.rpc.zookeeper.ThriftServerAddressProvider;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.async.TAsyncClientFactory;
import org.apache.thrift.transport.TNonblockingSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 连接池,thrift-client for spring
 */
public class ThriftAsyncClientPoolFactory extends BasePoolableObjectFactory<TAsyncClient> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private final ThriftServerAddressProvider serverAddressProvider;
	private final TAsyncClientFactory<TAsyncClient> clientFactory;
	private PoolOperationCallBack callback;

	protected ThriftAsyncClientPoolFactory(ThriftServerAddressProvider addressProvider, TAsyncClientFactory<TAsyncClient> clientFactory) throws Exception {
		this.serverAddressProvider = addressProvider;
		this.clientFactory = clientFactory;
	}

	protected ThriftAsyncClientPoolFactory(ThriftServerAddressProvider addressProvider, TAsyncClientFactory<TAsyncClient> clientFactory,
										   PoolOperationCallBack callback) throws Exception {
		this.serverAddressProvider = addressProvider;
		this.clientFactory = clientFactory;
		this.callback = callback;
	}

	@Override
	public TAsyncClient makeObject() throws Exception {
		InetSocketAddress address = serverAddressProvider.selector();
		if(address==null){
			new ThriftException("No provider available for remote service");
		}
		TNonblockingSocket transport = new TNonblockingSocket(address.getHostString(), address.getPort());
		TAsyncClient client = this.clientFactory.getAsyncClient(transport);
		if (callback != null) {
			try {
				callback.make(client);
			} catch (Exception e) {
				logger.warn("makeObject:{}", e);
			}
		}
		return client;
	}


	static interface PoolOperationCallBack {
		// 销毁client之前执行
		void destroy(TAsyncClient client);

		// 创建成功是执行
		void make(TAsyncClient client);
	}

}
