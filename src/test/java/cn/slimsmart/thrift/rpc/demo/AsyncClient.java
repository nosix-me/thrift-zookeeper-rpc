package cn.slimsmart.thrift.rpc.demo;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Calendar;


//客户端调用
@SuppressWarnings("resource")
public class AsyncClient {
	public static void main(String[] args) {
		//simple();
		spring();
	}

	public static void spring() {
		try {
			final ApplicationContext context = new ClassPathXmlApplicationContext("spring-context-thrift-asyncclient.xml");
			EchoSerivce.AsyncIface echoSerivce = (EchoSerivce.AsyncIface) context.getBean("echoService");
			long stime = Calendar.getInstance().getTimeInMillis();
			for(int i = 0; i < 1000; i++) {
				try{
					echoSerivce.echo("nosix", new MyCallback());
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			Thread.sleep(10000);
			System.out.println("程序运行耗时:"+(Calendar.getInstance().getTimeInMillis() - stime));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	public static void simple() {
//		try {
//			TSocket socket = new TSocket("192.168.36.215", 9001);
//			TTransport transport = new TFramedTransport(socket);
//			TProtocol protocol = new TBinaryProtocol(transport);
//			HelloService.Client client = new HelloService.Client(protocol);
//			transport.open();
//			System.out.println(client.hello("helloword"));
//			Thread.sleep(3000);
//			transport.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
