import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    private Socket clientSocket;
    private BufferedReader inFromServer;
	private DataOutputStream outToServer;
    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

		clientSocket = new Socket(server, port);
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        if(message.length() != 0) {
            message = processMessage(message);
			byte[] msgBytes = message.getBytes();
			outToServer.write(msgBytes, 0, msgBytes.length);
			outToServer.flush();
		}
    }

    
    // Método principal do objecto
    public void run() throws IOException {
        boolean connected = true;

		while(connected)
		{
            String msgFromServer = inFromServer.readLine();
			String[] tokens = msgFromServer.split(" ");
            String text;
			switch (tokens[0])
			{
				case "MESSAGE":		
                    text = tokens[1] + ": ";
                    for(int i  = 2; i < tokens.length; i++)
                        text += i == tokens.length - 1 ? tokens[i] : tokens[i] + " ";
                        break;
				case "NEWNICK":	
                    text = tokens[1] + " changed nick to " + tokens[2];
                    break;
                case "JOINED":      
                    text = tokens[1] + " joined";
                    break;
				case "BYE":		
                    connected = false;
					text = "BYE";
					break;
                case "LEFT":		
                    text = tokens[1] + " left";
					break;
                case "PRIVATE":     
                    text = "(Private) " + tokens[1] + ": ";
                    for(int i  = 2; i < tokens.length; i++)
                        text += i == tokens.length - 1 ? tokens[i] : tokens[i] + " ";
                    break;
				default:		
                    text = msgFromServer;
					break;
			}
			printMessage(text + "\n");
		}
		clientSocket.close();
		System.exit(0);
    }

    public String processMessage(String text) {
        return text.charAt(0) != '/' || isCommand(text.split(" ")[0]) ? text + "\n" : "/" + text + "\n";
    }
    
    public boolean isCommand(String text) {
        switch(text) {
            case "/nick":
            case "/join":
            case "/leave":
            case "/bye":
            case "/priv":
                return true;
            default:
                return false;
        }
    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}