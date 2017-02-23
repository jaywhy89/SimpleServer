import java.util.*;
import java.net.*;
import java.io.*;

class WebServer implements Runnable{

	Socket clientSocket;

	WebServer(Socket clientSocket) {
		this.clientSocket=clientSocket;
	}

	public void run() {
		try {
			final String PROXY = "/proxy/";
			final String CL = "Content-Length: ";
			final String UA = "User-Agent: ";
			final String ACCEPT = "Accept: ";
			int cl = 0;
			String userAgent = "";
			String accept = "";
			String formData = "";

			BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintStream out = new PrintStream(clientSocket.getOutputStream());

			/* originalReq is raw request text.
			*  Had to build string because StringBuilder will hang with POST method within while loop
			*/
			StringBuilder originalReq = new StringBuilder();

			String req = br.readLine();
			String address = findURL(req, PROXY);
			originalReq.append(req+"\n");
			boolean isPOST = req.startsWith("POST");

			while (!(req=br.readLine()).equals("")) {
				originalReq.append(req+"\n");

				// Search and store User-Agent, Accept, Content-Length headers.
				if (req.startsWith(UA)) {
					userAgent=req.substring(UA.length()).trim();
				} else if (req.startsWith(ACCEPT)) {
					accept=req.substring(ACCEPT.length()).trim();
				}

				if (isPOST && req.startsWith(CL)) {
					cl=Integer.parseInt(req.substring(CL.length()).trim());
				}
			}
			originalReq.append("");

			StringBuilder sb = new StringBuilder();
			
			// For POST request, find form data
			if (isPOST) {
				for (int i=0; i<cl; i++) {
					sb.append((char) br.read());
				}
				formData = sb.toString();
				originalReq.append("\n"+sb);
			}
			
			// System.out.println(originalReq.toString());

			/* Send request to specified url */

			URL url = new URL(address);
			HttpURLConnection uc = (HttpURLConnection) url.openConnection();
			
			/* Set request headers 
			*  Note: Set user-agent header to the same user-agent that is being called by.
			*/
			uc.setRequestProperty("User-Agent", userAgent);
			uc.setRequestProperty("Accept", accept);
			uc.setRequestProperty("Accept-Encoding", "gzip, deflate");

			if (isPOST) {
				uc.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				uc.setRequestMethod("POST");
				uc.setDoOutput(true);
				DataOutputStream data = new DataOutputStream(uc.getOutputStream());
				data.writeBytes(formData);
				data.flush();
				data.close();
			} else {
				uc.setRequestMethod("GET");
			}

			// Get response code
			System.out.println("response code: "+uc.getResponseCode());
	
			br = new BufferedReader(new InputStreamReader (uc.getInputStream()));
			String response;

			out.println();

			// Send response to client
			while ((response=br.readLine()) != null) {
				out.println(response);
			}

			// Close all
			out.close();
			br.close();
			clientSocket.close();
		
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	/* Parse string and return URL */
	private String findURL(String str, String proxy) {
		String[] parts=str.split("\\s+");
		return parts[1].substring(proxy.length());
	}

	public static void main (String[] args) throws Exception {
		ServerSocket server = new ServerSocket(8000);
		System.out.println("Starting Server.. Listening on port "+server.getLocalPort()+"\n");

		/* Using threads, so it can handle multiple clients */
		while (true) {
			Socket client = server.accept();
			System.out.println("Connected to: "+client.getInetAddress());
			new Thread(new WebServer(client)).start();
		}
	}
}