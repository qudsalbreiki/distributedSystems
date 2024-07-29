package com.example.comp5504_hw2_133343;
/*
 * This is a simple Group chat application where multiple clients join the group.
 * This code implemented by using UDP and Multicast sockets.
 * Also, threads are used here to ensure the concurrent execution.
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;

public class Server {
    // Initialize constants.
    private static final int PORT = 5000;
    private static final String PASSWORD = "secretPass";
    private static final HashMap<String, InetAddress> clients = new HashMap<>();

    public static void main(String[] args) {
        try {
            //Creating a multicastGroup and joining it
            InetAddress group = InetAddress.getByName("239.0.0.1");
            MulticastSocket multicastSocket = new MulticastSocket(PORT);
            multicastSocket.joinGroup(group);

            //Printing confirmation that the server running
            System.out.println("Server started a multicast group");

            //Receive packets
            while (true) {
                byte[] buffer = new byte[1000];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                // Create a thread for each client
                new Thread(new ClientHandler(multicastSocket, packet)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        //Initialize constants.
        private MulticastSocket multicastSocket;
        private DatagramPacket packet;

        public ClientHandler(MulticastSocket multicastSocket, DatagramPacket packet) {
            this.multicastSocket = multicastSocket;
            this.packet = packet;
        }

        @Override
        public void run() {
            // Real handling requests
            try {
                String[] requestData = new String(packet.getData(), 0, packet.getLength()).split(":");

                if (requestData.length != 2) {
                    sendResponse("rejected", packet.getAddress(), packet.getPort());
                    return;
                }

                // Receive client data
                String password = requestData[0];
                String nickname = requestData[1];

                // For wrong password
                if (!password.equals(PASSWORD)) {
                    sendResponse("rejected", packet.getAddress(), packet.getPort());
                    return;
                }

                // Check the nicknames
                synchronized (clients) {
                    // Reject if the same
                    if (clients.containsKey(nickname)) {
                        sendResponse("rejected", packet.getAddress(), packet.getPort());
                        return;
                    }
                    // Add the new nicknames
                    clients.put(nickname, packet.getAddress());
                }

                // Send Response
                sendResponse("accepted", packet.getAddress(), packet.getPort());

                System.out.println("Correct password from a new client: " + nickname);

                // Now handle client messages and broadcasting
                while (true) {
                    byte[] buffer = new byte[1000];
                    DatagramPacket messagePacket = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(messagePacket);
                    String message = new String(messagePacket.getData(), 0, messagePacket.getLength());

                    // Handle messages here
                    if (message.length() <= 100) {
                        // Broadcast message to other clients
                        DatagramPacket broadcastPacket = new DatagramPacket(message.getBytes(),
                                message.getBytes().length, packet.getAddress(), packet.getPort());
                        multicastSocket.send(broadcastPacket);
                    } else {
                        String errorMessage = "tooLong";
                        DatagramPacket errorPacket = new DatagramPacket(errorMessage.getBytes(),
                                errorMessage.getBytes().length, packet.getAddress(), packet.getPort());
                        multicastSocket.send(errorPacket);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Function to send response to clients
        private void sendResponse(String response, InetAddress address, int port) throws IOException {
            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address, port);
            multicastSocket.send(responsePacket);
        }
    }
}
