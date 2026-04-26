package servidor;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor extends JFrame {

    private static final long serialVersionUID = 1L;
	private JTextField campoPorta;
    private JButton btnIniciar, btnParar;
    private JTextArea areaLog;

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private final GerenciadorUsuarios gerenciador = new GerenciadorUsuarios();
    private final Set<Socket> conexoesAtivas = ConcurrentHashMap.newKeySet();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Servidor() {
        super("Servidor de Chat");
        inicializarGUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
    }

    private void inicializarGUI() {
        setLayout(new BorderLayout(5, 5));

        JPanel painelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        painelSuperior.add(new JLabel("Porta:"));
        campoPorta = new JTextField("12345", 8);
        painelSuperior.add(campoPorta);
        btnIniciar = new JButton("Iniciar Servidor");
        btnParar = new JButton("Parar Servidor");
        btnParar.setEnabled(false);
        painelSuperior.add(btnIniciar);
        painelSuperior.add(btnParar);
        add(painelSuperior, BorderLayout.NORTH);

        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createTitledBorder("Log do Servidor"));
        add(scroll, BorderLayout.CENTER);

        JPanel painelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnLimpar = new JButton("Limpar Log");
        btnLimpar.addActionListener(e -> areaLog.setText(""));
        painelInferior.add(btnLimpar);
        add(painelInferior, BorderLayout.SOUTH);

        btnIniciar.addActionListener(e -> iniciarServidor());
        btnParar.addActionListener(e -> pararServidor());
    }

    private void iniciarServidor() {
        String portaStr = campoPorta.getText().trim();
        int porta;
        try {
            porta = Integer.parseInt(portaStr);
            if (porta < 1 || porta > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porta inválida. Use um número entre 1 e 65535.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        executor = Executors.newCachedThreadPool();
        final int portaFinal = porta;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(portaFinal);
                log("Servidor iniciado na porta " + portaFinal + ".");
                SwingUtilities.invokeLater(() -> {
                    btnIniciar.setEnabled(false);
                    btnParar.setEnabled(true);
                    campoPorta.setEnabled(false);
                });
                while (!serverSocket.isClosed()) {
                    try {
                        Socket cliente = serverSocket.accept();
                        executor.submit(new ClienteHandler(cliente, gerenciador, this::log, conexoesAtivas));
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            log("[Erro ao aceitar conexão] " + e.getMessage() + ".");
                        }
                    }
                }
            } catch (IOException e) {
                log("[Erro ao iniciar servidor] " + e.getMessage() + ".");
            }
        }).start();
    }

    private void pararServidor() {
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException e) {
            log("[Erro ao fechar serverSocket] " + e.getMessage() + ".");
        }

        int totalDerrubadas = conexoesAtivas.size();
        for (Socket s : conexoesAtivas) {
            try { s.close(); } catch (IOException ignored) {}
        }
        conexoesAtivas.clear();
        if (totalDerrubadas > 0) {
            log("Encerradas " + totalDerrubadas + " conexão(ões) ativa(s).");
        }

        if (executor != null) executor.shutdownNow();
        log("Servidor parado.");

        SwingUtilities.invokeLater(() -> {
            btnIniciar.setEnabled(true);
            btnParar.setEnabled(false);
            campoPorta.setEnabled(true);
        });
    }

    private void log(String msg) {
        String ts = LocalDateTime.now().format(FMT);
        SwingUtilities.invokeLater(() -> {
            areaLog.append("[" + ts + "] " + msg + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Servidor().setVisible(true));
    }
}
