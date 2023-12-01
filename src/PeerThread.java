public class PeerThread extends Thread{

    Peer peer;
    PeerThread(Peer p){
        peer = p;
    }
    public void run()
    {
        // Convert to milliseconds
        int unchokingInterval = peer.unchokingInterval * 1000;
        int optimisticUnchokingInterval = peer.optimisticUnchokingInterval * 1000;
        while (true) {
            if (unchokingInterval < optimisticUnchokingInterval) {
                try {
                    Thread.sleep(unchokingInterval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Determine preferred neighbors
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
                // Determine preferred neighbors

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
