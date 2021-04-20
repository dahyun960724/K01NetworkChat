package chat6;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiServer {

	//멤버변수
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	//클라이언트 정보저장을 위한 Map 컬렉션 생성
	Map<String, PrintWriter> clientMap; //새로생김
	
	//생성자 
	public MultiServer() {
		//클라이언트의 이름과 출력스트림을 저장할 HashMap 컬렉션 생성
		clientMap = new HashMap<String, PrintWriter>();
		//HashMap 동기화 설정. 쓰레드가 사용자정보에 동시 접근하는것을 차단함
		Collections.synchronizedMap(clientMap);
	}
	
	//채팅 서버 초기화
	public void init() {
		try {
			//서버 소켓 오픈
			serverSocket = new ServerSocket(9999);//서버를 열고 기다림
			System.out.println("서버가 시작되었습니다.");
			//다현:서버소켓을 생성을하면 클라이언트가 접속할때까지 무한대기(커서만 뻑뻑뻑)하다 들어오면 허용
			
			/*
			1명의 클라이언트가 접속할때마다 접속을 허용(Accept)해주고
			동시에 MultiServerT 쓰레드를 생성한다.
			해당 쓰레드는 1명의 클라이언트가 전송하는 메세지를 읽어서 
			Echo 해주는 역할을 담당한다.
			 */
			while(true) {
				//클라이언트의 접속 허가
				socket = serverSocket.accept();//접속처리
				System.out.println(socket.getInetAddress() +"(클라이언트)의"
						+socket.getPort()+"포트를 통해 "
						+socket.getLocalAddress()+"(서버)의 "
						+socket.getLocalPort()+ "포트로 연결되었습니다.");//서버쪽 정보가 출력되는것
				
				//쓰레드로 정의된 내부클래스 객체생성 및 시작
				//클라이언트 한명당 하나씩의 쓰레드가 생성된다.
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
	
	//접속된 모든 클라이언트 측으로 서버의 메세지를 Echo해주는 역할 담당
	public void sendAllMsg(String name, String msg) {
		//Map에 저장된 객체의 키값(대화명)을 먼저 얻어온다.
		Iterator<String> it = clientMap.keySet().iterator();
		
		//저장된 객체(클라이언트)의 갯수만큼 반복한다.
		while(it.hasNext()) {
			try {
				//각 클라이언트의 PrintWriter객체를 얻어온다.
				PrintWriter it_out = (PrintWriter)
				clientMap.get(it.next());
				
				/*
				클라이언트에게 메세지를 전달할때 매개변수로 name이 있는경우와
				없는경우를 구분해서 전달하게된다.
				 */
				if(name.equals("")) {
					//입장, 퇴장에서 사용되는 부분
					it_out.println(msg);
				}
				else {
					//메세지를 보낼때 사용되는 부분
					it_out.println("["+ name +"]:"+ msg);
				}
			}
			catch (Exception e) {
				System.out.println("예외:"+e);
			}
		}
	}
	/*
	내부클래스
		: init()에 기술되었던 스트림을 생성 후 메세지를 읽기/쓰기 하던
		부분이 해당 내부클래스로 이동되었다.
	 */
	class MultiServerT extends Thread {
		
		//멤버변수
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
					
					//클라이언트의 이름을 읽어온다.
					name = in.readLine();//readLine동작하는이유:스트림이 메세지를 보내면 그때 반응하기때문에 무한루프에 빠지지않음
					/*
					방금 접속한 클라이언트를 제외한 나머지에게 입장을 알린다.
					 */
					sendAllMsg("", name + "님이 입장하셨습니다."  );//클라이언트 전체한테(sendAll이기에)
					//현재 접속한 클라이언트를 HashMap에 저장한다.
					clientMap.put(name, out);
					
					//접속자의 이름을 서버의 콘솔에 띄워주고
					System.out.println(name + "접속");
					//HashMap에 저장된 객체의 수로 현재 접속자를 파악할 수 있다.
					System.out.println("현재 접속자 수는 "+clientMap.size()+"명 입니다.");
					
					/*
					입력한 메세지는 모든 클라이언트에게 Echo된다.
					두번째부터는 실제 메세지이므로 지속적으로 읽어서 Echo한다.
					 */
					while(in != null) {
						s = in.readLine();
						if(s == null) 
							break;
						//서버의 콘솔에 출력된다.
						System.out.println(name +" >> "+ s);
						//클라이언트 측으로 전송한다.
						sendAllMsg(name, s);
					}
				}
			}
			catch (Exception e) {
				System.out.println("예외:"+ e);
			}
			finally {
				/*
				클라이언트가 접속을 종료하면 Socket예외가 발생하게되어
				finally절로 진입하게된다. 이때 "대화명"을 통해 정보를 
				삭제한다.
				 */
				clientMap.remove(name);
				sendAllMsg("", name + "님이 퇴장하셨습니다.");
				System.out.println(name + " ["+ Thread.currentThread().getName() + "] 퇴장");
				System.out.println("현재 접속자 수는 "+clientMap.size()+"명 입니다.");
//				
				//객체정보 반환
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
	}

}
