package dev.CarlosMendoza;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class mydns {

    final String testDomain = "cs.fiu.edu";
    final String testdnsServer = "202.12.27.33";
    final int dnsPort = 53;

    public static void main(String[] args) {
        mydns mydns = new mydns(args[0], args[1]);
    }

    public mydns(String domain, String dnsServer){
        try {
            //test();
//            dnsRequest(testDomain,testdnsServer);
//            dnsRequest("fiu.edu","8.8.8.8");
//            dnsRequest("google.com", "8.8.8.8");
            dnsRequest(domain, dnsServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean dnsRequest(String domain, String dnsServer) throws IOException {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteOutputStream);

        //transaction ID
        dataOutputStream.writeShort(0x5555);
        //Query Flags
        dataOutputStream.writeShort(0x0000);
        //Question #
        dataOutputStream.writeShort(0x0001);
        //Answers #
        dataOutputStream.writeShort(0x0000);
        //Authority #
        dataOutputStream.writeShort(0x0000);
        //Additional #
        dataOutputStream.writeShort(0x0000);
        
        String[] domainTokens = domain.split("\\.");
        for (String token: domainTokens) {
            byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
            dataOutputStream.writeByte(bytes.length);
            dataOutputStream.write(bytes);
        }

        //No more Questions
        dataOutputStream.writeByte(0x00);
        //Type A
        dataOutputStream.writeShort(0x0001);
        //Class IN
        dataOutputStream.writeShort(0x0001);

        byte[] dnsRequestMessage = byteOutputStream.toByteArray();

        System.out.println("----------------------------------------------------------------");
        System.out.println("DNS server to query: "+dnsServer);

        DatagramSocket socket = new DatagramSocket();
        InetAddress ip = InetAddress.getByName(dnsServer);
        DatagramPacket dnsRequestPacket = new DatagramPacket(dnsRequestMessage, dnsRequestMessage.length, ip, dnsPort);
        socket.send(dnsRequestPacket);

        // Await response from DNS server
        byte[] buffer = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(receivedPacket);

        System.out.println("Reply received. Content overview:");

        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(buffer));
        int transactionID = dataInputStream.readShort();
        short flags = dataInputStream.readShort();
        int questions = dataInputStream.readShort();
        int answers = dataInputStream.readShort();
        int authority = dataInputStream.readShort();
        int additional = dataInputStream.readShort();

        System.out.println("\t"+Integer.toString(answers)+" Answers.");
        System.out.println("\t"+Integer.toString(authority)+" Intermediate Name Servers.");
        System.out.println("\t"+Integer.toString(additional)+" Additional Information Records.");

        int queryLength = 0;
        String queryName = "";
        queryLength = dataInputStream.readByte();
        while (queryLength != 0){
            byte[] temp = new byte[queryLength];
            for (int i = 0; i < queryLength; i++) {
                temp[i] = dataInputStream.readByte();
            }
            queryName = queryName + new String(temp, StandardCharsets.UTF_8)+ ".";
            queryLength = dataInputStream.readByte();
        }
        //Type A
        dataInputStream.readShort();
        //Class IN
        dataInputStream.readShort();

        System.out.println("Answers Section:");
        for (int i = 0; i < answers; i++) {
            int length = dataInputStream.readByte();
            if ((length & 0xc0) == 0xc0){
                System.out.print("\tName: "+getPointerName(buffer, dataInputStream.readByte()));
            } else {
                queryName = "";
                while (length != 0){
                    if ((length & 0xc0) == 0xc0){
                        queryName = queryName + getPointerName(buffer, dataInputStream.readByte());
                        break;
                    }
                    System.out.println(length & 0xFF);
                    byte[] temp = new byte[length & 0xFF];
                    for (int j = 0; j < length; j++) {
                        temp[j] = dataInputStream.readByte();
                    }
                    queryName = queryName + new String(temp, StandardCharsets.UTF_8)+ ".";
                    length = dataInputStream.readByte();
                }
                System.out.print("\tName: "+queryName.substring(0,queryName.length()-1));
            }
            //NS
            dataInputStream.readShort();
            //Class In
            dataInputStream.readShort();
            //TTL
            dataInputStream.readInt();
            //Data Length
            dataInputStream.readShort();

            length = dataInputStream.readByte() & 0xFF;
            if ((length & 0xc0) == 0xc0){
                System.out.print("\tName Server: "+getPointerName(buffer, dataInputStream.readByte() & 0xFF));
            } else {
                queryName = "";
                while (length != 0){
//                    byte[] temp = new byte[length & 0xFF];
//                    for (int j = 0; j < length; j++) {
//                        temp[j] = dataInputStream.readByte();
//                    }
                    queryName = queryName + length + ".";
                    length = dataInputStream.readByte() & 0xFF;
                }
                if (queryName.endsWith(".")) {
                    System.out.print("\tName Server: " + queryName.substring(0, queryName.length()-1));
                } else {
                    System.out.print("\tName Server: " + queryName.substring(0, queryName.length()));
                }
            }
            System.out.println();
        }

        System.out.println("Authoritative Section:");
        for (int i = 0; i < authority; i++) {
            int length = dataInputStream.readByte();
            if ((length & 0xc0) == 0xc0){
                System.out.print("\tName: "+getPointerName(buffer, dataInputStream.readByte()));
            } else {
                queryName = "";
                while (length != 0){
                    if ((length & 0xc0) == 0xc0){
                        queryName = queryName + getPointerName(buffer, dataInputStream.readByte());
                        break;
                    }
                    System.out.println(length & 0xFF);
                    byte[] temp = new byte[length & 0xFF];
                    for (int j = 0; j < length; j++) {
                        temp[j] = dataInputStream.readByte();
                    }
                    queryName = queryName + new String(temp, StandardCharsets.UTF_8)+ ".";
                    length = dataInputStream.readByte();
                }
                System.out.print("\tName: "+queryName.substring(0,queryName.length()-1));
            }
            //NS
            dataInputStream.readShort();
            //Class In
            dataInputStream.readShort();
            //TTL
            dataInputStream.readInt();
            //Data Length
            dataInputStream.readShort();

            length = dataInputStream.readByte() & 0xFF;
            if ((length & 0xc0) == 0xc0){
                System.out.print("\tName Server: "+getPointerName(buffer, dataInputStream.readByte() & 0xFF));
            } else {
                queryName = "";
                while (length != 0){
                    if ((length & 0xc0) == 0xc0){
                        queryName = queryName + getPointerName(buffer, dataInputStream.readByte() & 0xFF);
                        break;
                    }
                    byte[] temp = new byte[length & 0xFF];
                    for (int j = 0; j < length; j++) {
                        temp[j] = dataInputStream.readByte();
                    }
                    queryName = queryName + new String(temp, StandardCharsets.UTF_8)+ ".";
                    length = dataInputStream.readByte() & 0xFF;
                }
                if (queryName.endsWith(".")) {
                    System.out.print("\tName Server: " + queryName.substring(0, queryName.length()-1));
                } else {
                    System.out.print("\tName Server: " + queryName.substring(0, queryName.length()));
                }
            }
            System.out.println();
        }

        System.out.println("Additional Information Section:");

        for (int i = 0; i < additional; i++) {
            int length = dataInputStream.readByte() & 0xFF;
//           if ((length & 0xc0) == 0xc0){
            System.out.print("\tName: " + getPointerName(buffer, dataInputStream.readByte()));

//           } else {
//                queryName = "";
//                while (length != 0){
//                    if ((length & 0xc0) == 0xc0){
//                        queryName = queryName + getPointerName(buffer, dataInputStream.readByte());
//                        break;
//                    }
//                    byte[] temp = new byte[length & 0xFF];
//                    for (int j = 0; j < length; j++) {
//                        temp[j] = dataInputStream.readByte();
//                    }
//                    queryName = queryName + new String(temp, StandardCharsets.UTF_8)+ ".";
//                    length = dataInputStream.readByte();
//                }
//                System.out.println("Name: "+queryName.substring(0,queryName.length()));
//            }

//        for (int i = 0; i < additional; i++) {
//            int length = dataInputStream.readByte();
//            if ((length & 0xc0) == 0xc0){
//                System.out.println(getPointerName(buffer, dataInputStream.readByte()));
//            } else {
//                queryName = "";
//                while (length != 0){
//                    if ((length & 0xc0) == 0xc0){
//                        queryName = queryName + getPointerName(buffer, dataInputStream.readByte());
//                        break;
//                    }
//                    byte[] temp = new byte[length & 0xFF];
//                    for (int j = 0; j < length; j++) {
//                        temp[j] = dataInputStream.readByte();
//                    }
//                    queryName = queryName + new String(temp, StandardCharsets.UTF_8)+ ".";
//                    length = dataInputStream.readByte();
//                }
//                System.out.println(queryName.substring(0,queryName.length()));
//            }
//
            //NS
            dataInputStream.readShort();
            //Class In
            dataInputStream.readShort();
            //TTL
            dataInputStream.readInt();
            //Data Length
            dataInputStream.readShort();

            length = dataInputStream.readByte() & 0xFF;

                queryName = "";
                while ((length & 0xff) != 0x00){
//                    byte[] temp = new byte[length & 0xFF];
//                    for (int j = 0; j < length; j++) {
//                        temp[j] = dataInputStream.readByte();
//                    }
                    queryName = queryName + length + ".";
                    length = dataInputStream.readByte() & 0xFF;
                }
                if (queryName.endsWith(".")) {
                    System.out.print("\tName Server: " + queryName.substring(0, queryName.length()-1));
                } else {
                    System.out.print("\tName Server: " + queryName.substring(0, queryName.length()));
                }

            System.out.println();
        }

//
//            length = dataInputStream.readByte() & 0xFF;
//            if ((length & 0xc0) == 0xc0){
//                System.out.print("\tName Server: "+getPointerName(buffer, dataInputStream.readByte() & 0xFF));
//            } else {
//                queryName = "";
//                while (length != 0){
//                    if ((length & 0xc0) == 0xc0){
//                        queryName = queryName + getPointerName(buffer, dataInputStream.readByte() & 0xFF);
//                        break;
//                    }
////                    byte[] temp = new byte[length & 0xFF];
////                    for (int j = 0; j < length; j++) {
////                        temp[j] = dataInputStream.readByte();
////                    }
//                    queryName = queryName + length + ".";
//                    length = dataInputStream.readByte() & 0xFF;
//                }
//                if (queryName.endsWith(".")) {
//                    System.out.print("\tName Server: " + queryName.substring(0, queryName.length()-1));
//                } else {
//                    System.out.print("\tName Server: " + queryName.substring(0, queryName.length()));
//                }
//            }
//            System.out.println();
//
        return true;
    }

    String getPointerName(byte[] data, int index) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data));
        dataInputStream.skipBytes(index);
//
//        int queryLength = 0;
        String queryName = "";
//        queryLength = dataInputStream.readByte();
////        if ((queryLength & 0xc0) == 0xc0){
////            System.out.print("\tName: "+getPointerName(data, dataInputStream.readByte()));
////        }
//        while (queryLength != 0){
//            byte[] temp = new byte[queryLength & 0xFF];
//            for (int i = 0; i < queryLength; i++) {
//                temp[i] = dataInputStream.readByte();
//            }
//            queryName = queryName + new String(temp, StandardCharsets.UTF_8)+ ".";
//            queryLength = dataInputStream.readByte();
//        }
//        return queryName.substring(0,queryName.length()-1);

        int length = dataInputStream.readByte();
        if ((length & 0xc0) == 0xc0){
            return getPointerName(data, dataInputStream.readByte());
        } else {
            queryName = "";
            while (length != 0){
                if ((length & 0xc0) == 0xc0){
                    queryName = queryName + getPointerName(data, dataInputStream.readByte());
                    break;
                }
                byte[] temp = new byte[length & 0xFF];
                for (int j = 0; j < length; j++) {
                    temp[j] = dataInputStream.readByte();
                }
                queryName = queryName + new String(temp, StandardCharsets.UTF_8)+ ".";
                length = dataInputStream.readByte();
            }
            return queryName.substring(0,queryName.length());
        }
    }
}
