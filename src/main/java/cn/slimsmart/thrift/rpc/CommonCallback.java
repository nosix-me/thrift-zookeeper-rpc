package cn.slimsmart.thrift.rpc;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.thrift.async.TAsyncClient;

public class CommonCallback{
	protected GenericObjectPool<TAsyncClient> pool;
	protected TAsyncClient client;
	
	public GenericObjectPool<TAsyncClient> getPool() {
		return pool;
	}

	public void setPool(GenericObjectPool<TAsyncClient> pool) {
		this.pool = pool;
	}

	public TAsyncClient getClient() {
		return client;
	}

	public void setClient(TAsyncClient client) {
		this.client = client;
	}
	
	protected void giveBackResrouce() throws Exception {
		if (pool != null && client != null) {
			pool.returnObject(client);
		}
	}
}
