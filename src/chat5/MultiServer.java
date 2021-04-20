package chat5;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiServer {

	//멤버변수
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	public MultiServer() {
		//생성자 실행부 없음
	}
	
	//채팅 서버 초기화
	public void init() {
		try {
			//서버 소켓 오픈
			serverSocket = new ServerSocket(9999);//서버를 열고 기다림
			System.out.println("서버가 시작되었습니다.");
			//다현:서버소켓을 생성을하면 클라이언트가 접속할때까지 
			//무한대기(커서만 뻑뻑뻑)하다 들어오면 허용
			/*
			1명의 클라이언트가 접속할때마다 접속을 허용(Accept)해주고
			동시에 MultiServerT 쓰레드를 생성한다.
			해당 쓰레드는 1명의 클라이언트가 전송하는 메세지를 읽어서 
			Echo 해주는 역할을 담당한다.
			 */
			while(true) {
				//클라이언트의 접속 허가
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress()+":"+socket.getPort());//정보를 콘솔에 잠깐띄워주고
				
				//쓰레드로 정의된 내부클래스 객체생성 및 시작
				Thread mst = new MultiServerT(socket);
				mst.start();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	chat4까지는 init()이 static이었으나 chat5부터는 일반적인 멤버메소드로
	변경.
	따라서 객체를 생성 후 호출하는 방식으로 변경된다.
	 */
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();//5단계의 init은 static타입이 아니라 4단계처럼 init(); 으로 호출 불가능
		//5단계를 static init으로 만들면 프로그램시작전 이미 올라가서 두개를만들수없음
	}
	
	/*
	내부클래스
		: init()에 기술되었던 스트림을 생성 후 메세지를 읽기/쓰기 하던
		부분이 해당 내부클래스로 이동되었다.
	 */
	class MultiServerT extends Thread {
		
		Socket socket;
		PrintWriter out = null;//out,in 2개의 io객체 생성
		BufferedReader in = null;
		/*
		내부클래스의 생성자
			: 1명의 클라이언트가 접속할때 생성했던 Socket객체를
			매개변수로 받아 이를 기반으로 입출력 스트림을 생성한다.
		 */
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(),true);
				in = new BufferedReader(new 
				InputStreamReader(this.socket.getInputStream()));
			}//위치만 달라지고 크게 변하지 않음
			catch (Exception e) {
				System.out.println("예외:"+ e);
			}
		}
		
		/*
		쓰레드로 동작할 run()에서는 클라이언트의 접속자명과 메세지를
		지속적으로 읽어 Echo해주는 역할을 한다.
		 */
		@Override
		public void run() {
			String name = "";
			String s = "";
			
			try {
				/*
				클라이언트가 보내는 최초메세지는 대화명이므로
				접속에 대한 부분을 출력하고 Echo한다.
				 */
				if(in != null) {
					name = in.readLine();//readLine동작하는이유:스트림이 메세지를 보내면 그때 반응하기때문에 무한루프에 빠지지않음
					
					System.out.println(name +"접속");//앍는부분?
					out.println("> "+ name +"님이 접속했습니다.");//Echo하는부분
				}
				/*
				두번째부터는 실제 메세지이므로 지속적으로 읽어서 
				Echo한다.
				 */
				while(in != null) {
					s = in.readLine();
					if(s == null) break;
					
					System.out.println(name +" >> "+ s);
					sendAllMsg(name, s);
				}
			}
			catch (Exception e) {
				System.out.println("예외:"+ e);
			}
			finally {
				System.out.println(Thread.currentThread().getName()+ " 종료");
				
				try {
					in.close();
					out.close();
					socket.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		//클라이언트 측으로 서버의 메세지를 Echo해주는 역할 담당
		public void sendAllMsg(String name, String msg) {
			try {
				out.println("> " + name + " ==> " + msg);
			}
			catch (Exception e) {
				System.out.println("예외:"+e);
			}
		}
	}

}
