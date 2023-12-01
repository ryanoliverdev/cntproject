public class PeerThread extends Thread{

    Peer peer;
    PeerThread(Peer p){
        peer = p;
    }
    public void run()
    {
        int unchokingInterval = peer.unchokingInterval;
        int optimisticUnchokingInterval = peer.optimisticUnchokingInterval;
        while (true) {
            if (unchokingInterval < optimisticUnchokingInterval) {
                try {
                    Thread.sleep(unchokingInterval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Perform unchoking operation
                try {
                    Thread.sleep(optimisticUnchokingInterval - unchokingInterval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Perform optimistic unchoking operation
            } else {
                try {
                    Thread.sleep(optimisticUnchokingInterval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Perform optimistic unchoking operation

                try {
                    Thread.sleep(unchokingInterval - optimisticUnchokingInterval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Perform unchoking operation
            }
        }

    }
}
