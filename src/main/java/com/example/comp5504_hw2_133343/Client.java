package com.example.comp5504_hw2_133343;
/*
 * Author: Quds Al Breiki -133343
 * This is a simple Group chat application where multiple clients join the group.
 * This code implemented by using UDP and Multicast sockets.
 * Also, threads are used here to ensure the concurrent execution.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Client {
    // Initialize constants.
    private static final int PORT = 5000;
    private static final String GROUP_ADDRESS = "239.0.0.1";

    public static void main(String[] args) {
        try {
            //Creating the same multicastGroup as server and joining it
            InetAddress group = InetAddress.getByName(GROUP_ADDRESS);
            MulticastSocket multicastSocket = new MulticastSocket(PORT);
            multicastSocket.joinGroup(group);

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            // Requesting client information
            System.out.print("Enter the password: ");
            String password = reader.readLine();

            System.out.print("Enter your desired nickname: ");
            String nickname = reader.readLine();

            // Creating a DatagramSocket
            DatagramSocket socket = new DatagramSocket();

            // Send password and nickname to server
            String requestData = password + ":" + nickname;
            DatagramPacket requestPacket = new DatagramPacket(requestData.getBytes(), requestData.getBytes().length, group, PORT);
            socket.send(requestPacket);

            // Receive authentication response from server
            byte[] authBuffer = new byte[100];
            DatagramPacket authPacket = new DatagramPacket(authBuffer, authBuffer.length);
            socket.receive(authPacket);
            String authResponse = new String(authPacket.getData(), 0, authPacket.getLength());

            if (authResponse.equals("accepted")) {
                System.out.println("Authentication successful.");

                // Athread for each client
                Thread receiveThread = new Thread(() -> {
                    try {
                        byte[] buffer = new byte[1000];
                        while (true) {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            multicastSocket.receive(packet);

                            String message = new String(packet.getData(), 0, packet.getLength());

                            // Check for server response regarding message length
                            if (message.equals("tooLong")) {
                                System.out.println("Message too long, cannot be forwarded.");
                            } else {
                                // Get sender nickname from message
                                String senderNickname = message.split(":")[0].trim();

                                // Check if the message is from a different client
                                if (!senderNickname.equals(nickname)) {
                                    System.out.println(message);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                receiveThread.start();

                while (true) {
                    // Terminate if received exit from a client
                    String message = reader.readLine();
                    if (message.equals("exit")) {
                        break;
                    }
                    if (message.length() <= 100) {
                        String wholeMessage = nickname + ": " + message;
                        DatagramPacket packet = new DatagramPacket(wholeMessage.getBytes(), wholeMessage.getBytes().length, group, PORT);
                        socket.send(packet);
                    } else {
                        System.out.println("Message too long. Maximum 100 characters allowed.");
                    }
                }
            } else {
                // Wrong password
                System.out.println("Authentication failed. Wrong password or nickname already in use.");
            }

            socket.close();
            multicastSocket.leaveGroup(group);
            multicastSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
