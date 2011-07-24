import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.Registry;


public class PeerServer implements Runnable{
	private PeerInterface pi;
	private Registry registry; 

		
	public PeerInterface getPi() {
		return pi;
	}

	public void setPi(PeerInterface pi) {
		this.pi = pi;
	}

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public PeerServer(PeerInterface p){
		pi = p;
		
	}

	@Override
	public void run() {

		try {
			System.out.println("Starting Server " + pi.getIp() + " on port: " + pi.getPort());
			 
			registry = java.rmi.registry.LocateRegistry.createRegistry(Integer.parseInt(pi.getPort()));
			 
		    Naming.rebind("rmi://"+pi.getIp()+":"+pi.getPort()+"/PeerService", pi);
		} catch (MalformedURLException e) {
		    System.out.println("Malformed URL: " + e);
		} catch (RemoteException e){
			System.out.println("Ran into remote exception: " + e);
		}
	}
	
}
