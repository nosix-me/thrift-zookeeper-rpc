package cn.slimsmart.thrift.rpc.demo;

import cn.slimsmart.thrift.rpc.CommonCallback;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

/**
 * @author 吕桂强
 * @email larry.lv.word@gmail.com
 * @version 创建时间：2012-4-25 上午11:17:32
 */
public class MyCallback extends CommonCallback implements AsyncMethodCallback<EchoSerivce.AsyncClient.echo_call> {

	@Override
	public void onComplete(EchoSerivce.AsyncClient.echo_call response) {
		System.out.println("onComplete");
		try {
			System.out.println(response.getResult().toString());
		} catch (TException e) {
			e.printStackTrace();
		} finally {
			try {
				giveBackResrouce();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 返回异常
	@Override
	public void onError(Exception exception) {
		exception.printStackTrace();
		System.out.println("onError");
	}

}