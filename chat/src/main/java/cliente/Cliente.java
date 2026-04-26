package cliente;

import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Cliente extends JFrame {

    private static final long serialVersionUID = 1L;
	private CardLayout cardLayout;
    private JPanel painelPrincipal;

    private JTextField campoIP, campoPortaConexao;
    private JButton btnConectar;

    private JTextField campoLoginUsuario;
    private JPasswordField campoLoginSenha;
    private JTextField campoCadNome, campoCadUsuario;
    private JPasswordField campoCadSenha;

    private JLabel lblNome, lblUsuario, lblToken;
    private JTextField campoAtuNome;
    private JPasswordField campoAtuSenha;

    private JTextArea areaLog;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String tokenAtual;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Cliente() {
        super("Cliente de Chat");
        inicializarGUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 620);
        setLocationRelativeTo(null);
    }

    private void inicializarGUI() {
        setLayout(new BorderLayout(5, 5));

        areaLog = new JTextArea(10, 60);
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log do Cliente"));
        add(scrollLog, BorderLayout.SOUTH);

        cardLayout = new CardLayout();
        painelPrincipal = new JPanel(cardLayout);
        painelPrincipal.add(criarPainelConexao(), "conexao");
        painelPrincipal.add(criarPainelAuth(), "auth");
        painelPrincipal.add(criarPainelUsuario(), "usuario");
        add(painelPrincipal, BorderLayout.CENTER);

        cardLayout.show(painelPrincipal, "conexao");
    }

    private JPanel criarPainelConexao() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Conectar ao Servidor"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("IP do Servidor:"), gbc);
        gbc.gridx = 1;
        campoIP = new JTextField("127.0.0.1", 18);
        painel.add(campoIP, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(new JLabel("Porta:"), gbc);
        gbc.gridx = 1;
        campoPortaConexao = new JTextField("12345", 18);
        painel.add(campoPortaConexao, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        btnConectar = new JButton("Conectar");
        btnConectar.addActionListener(e -> conectar());
        painel.add(btnConectar, gbc);

        return painel;
    }

    private JPanel criarPainelAuth() {
        JPanel painel = new JPanel(new BorderLayout(5, 5));
        painel.setBorder(BorderFactory.createTitledBorder("Autenticação"));

        JPanel centro = new JPanel(new GridLayout(1, 2, 15, 0));

        JPanel painelLogin = new JPanel(new GridBagLayout());
        painelLogin.setBorder(BorderFactory.createTitledBorder("Login"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        painelLogin.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1;
        campoLoginUsuario = new JTextField(14);
        painelLogin.add(campoLoginUsuario, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painelLogin.add(new JLabel("Senha (6 dígitos):"), gbc);
        gbc.gridx = 1;
        campoLoginSenha = new JPasswordField(14);
        painelLogin.add(campoLoginSenha, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton btnLogin = new JButton("Entrar");
        btnLogin.addActionListener(e -> login());
        painelLogin.add(btnLogin, gbc);

        centro.add(painelLogin);

        JPanel painelCad = new JPanel(new GridBagLayout());
        painelCad.setBorder(BorderFactory.createTitledBorder("Cadastrar Novo Usuário"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        painelCad.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1;
        campoCadNome = new JTextField(14);
        painelCad.add(campoCadNome, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painelCad.add(new JLabel("Usuário (5-20):"), gbc);
        gbc.gridx = 1;
        campoCadUsuario = new JTextField(14);
        painelCad.add(campoCadUsuario, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        painelCad.add(new JLabel("Senha (6 dígitos):"), gbc);
        gbc.gridx = 1;
        campoCadSenha = new JPasswordField(14);
        painelCad.add(campoCadSenha, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton btnCadastrar = new JButton("Cadastrar");
        btnCadastrar.addActionListener(e -> cadastrar());
        painelCad.add(btnCadastrar, gbc);

        centro.add(painelCad);
        painel.add(centro, BorderLayout.CENTER);

        JPanel inferior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnDesconectar = new JButton("Desconectar");
        btnDesconectar.addActionListener(e -> desconectar());
        inferior.add(btnDesconectar);
        painel.add(inferior, BorderLayout.SOUTH);

        return painel;
    }

    private JPanel criarPainelUsuario() {
        JPanel painel = new JPanel(new BorderLayout(5, 5));
        painel.setBorder(BorderFactory.createTitledBorder("Usuário Logado"));

        JPanel painelInfo = new JPanel(new GridBagLayout());
        painelInfo.setBorder(BorderFactory.createTitledBorder("Dados do Cadastro"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        painelInfo.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1;
        lblNome = new JLabel("-");
        lblNome.setFont(lblNome.getFont().deriveFont(Font.BOLD));
        painelInfo.add(lblNome, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painelInfo.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1;
        lblUsuario = new JLabel("-");
        painelInfo.add(lblUsuario, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        painelInfo.add(new JLabel("Token:"), gbc);
        gbc.gridx = 1;
        lblToken = new JLabel("-");
        painelInfo.add(lblToken, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton btnConsultar = new JButton("Consultar Meus Dados");
        btnConsultar.addActionListener(e -> consultarUsuario());
        painelInfo.add(btnConsultar, gbc);

        painel.add(painelInfo, BorderLayout.NORTH);

        JPanel painelAtualizar = new JPanel(new GridBagLayout());
        painelAtualizar.setBorder(BorderFactory.createTitledBorder("Atualizar Dados"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        painelAtualizar.add(new JLabel("Novo Nome:"), gbc);
        gbc.gridx = 1;
        campoAtuNome = new JTextField(16);
        painelAtualizar.add(campoAtuNome, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painelAtualizar.add(new JLabel("Nova Senha (6 dígitos):"), gbc);
        gbc.gridx = 1;
        campoAtuSenha = new JPasswordField(16);
        painelAtualizar.add(campoAtuSenha, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton btnAtualizar = new JButton("Atualizar");
        btnAtualizar.addActionListener(e -> atualizarUsuario());
        painelAtualizar.add(btnAtualizar, gbc);

        painel.add(painelAtualizar, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        JButton btnDeletar = new JButton("Deletar Minha Conta");
        btnDeletar.setForeground(Color.RED);
        btnDeletar.addActionListener(e -> deletarUsuario());

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> logout());

        painelBotoes.add(btnDeletar);
        painelBotoes.add(btnLogout);
        painel.add(painelBotoes, BorderLayout.SOUTH);

        return painel;
    }

    private static final int TIMEOUT_CONEXAO_MS = 5000;

    private void conectar() {
        String ip = campoIP.getText().trim();
        String portaStr = campoPortaConexao.getText().trim();
        
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o IP do servidor.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int porta;
        try {
            porta = Integer.parseInt(portaStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Porta inválida: '" + portaStr + "' não é um número.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (porta < 1 || porta > 65535) {
            JOptionPane.showMessageDialog(this,
                "Porta fora do intervalo permitido (1-65535). Recebido: " + porta + ".",
                "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnConectar.setEnabled(false);
        btnConectar.setText("Conectando...");
        log("[Conexão] Tentando conectar em " + ip + ":" + porta + " (timeout " + (TIMEOUT_CONEXAO_MS / 1000) + "s)...");

        final int portaFinal = porta;
        new Thread(() -> {
            Socket s = new Socket();
            try {
                s.connect(new java.net.InetSocketAddress(ip, portaFinal), TIMEOUT_CONEXAO_MS);
                BufferedReader inLocal = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
                PrintWriter outLocal = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
                SwingUtilities.invokeLater(() -> {
                    socket = s;
                    in = inLocal;
                    out = outLocal;
                    log("[Conexão] Conectado ao servidor " + ip + ":" + portaFinal + ".");
                    cardLayout.show(painelPrincipal, "auth");
                    btnConectar.setEnabled(true);
                    btnConectar.setText("Conectar");
                });
            } catch (java.net.SocketTimeoutException ex) {
                try { s.close(); } catch (IOException ignored) {}
                SwingUtilities.invokeLater(() -> {
                    log("[Conexão] Timeout: " + ip + ":" + portaFinal + " não respondeu em " + (TIMEOUT_CONEXAO_MS / 1000) + "s.");
                    JOptionPane.showMessageDialog(this,
                        "Timeout: o servidor " + ip + ":" + portaFinal + " não respondeu em "
                        + (TIMEOUT_CONEXAO_MS / 1000) + " segundos.\n"
                        + "Verifique se o IP está correto e se o servidor está no ar.",
                        "Erro de conexão", JOptionPane.ERROR_MESSAGE);
                    btnConectar.setEnabled(true);
                    btnConectar.setText("Conectar");
                });
            } catch (java.net.UnknownHostException ex) {
                try { s.close(); } catch (IOException ignored) {}
                SwingUtilities.invokeLater(() -> {
                    log("[Conexão] Host desconhecido: " + ip + ".");
                    JOptionPane.showMessageDialog(this,
                        "Host desconhecido: '" + ip + "'.\nVerifique o IP ou nome do servidor.",
                        "Erro de conexão", JOptionPane.ERROR_MESSAGE);
                    btnConectar.setEnabled(true);
                    btnConectar.setText("Conectar");
                });
            } catch (java.net.ConnectException ex) {
                try { s.close(); } catch (IOException ignored) {}
                SwingUtilities.invokeLater(() -> {
                    log("[Conexão] Recusada: " + ex.getMessage() + ".");
                    JOptionPane.showMessageDialog(this,
                        "Conexão recusada por " + ip + ":" + portaFinal + ".\n"
                        + "Verifique se o servidor está rodando e ouvindo nessa porta.",
                        "Erro de conexão", JOptionPane.ERROR_MESSAGE);
                    btnConectar.setEnabled(true);
                    btnConectar.setText("Conectar");
                });
            } catch (Exception ex) {
                try { s.close(); } catch (IOException ignored) {}
                SwingUtilities.invokeLater(() -> {
                    log("[Conexão] Erro: " + ex.getClass().getSimpleName() + " - " + ex.getMessage() + ".");
                    JOptionPane.showMessageDialog(this,
                        "Não foi possível conectar:\n" + ex.getMessage(),
                        "Erro de conexão", JOptionPane.ERROR_MESSAGE);
                    btnConectar.setEnabled(true);
                    btnConectar.setText("Conectar");
                });
            }
        }, "conectar-cliente").start();
    }

    private void desconectar() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            log("[Conexão] Desconectado do servidor.");
        } catch (IOException ignored) {}
        tokenAtual = null;
        cardLayout.show(painelPrincipal, "conexao");
    }

    private JSONObject enviarEReceber(JSONObject json) {
        String texto = json.toString();
        log("[Enviado] " + texto);
        out.println(texto);
        
        try {
            String resposta = in.readLine();
            if (resposta == null) return null;
            log("[Recebido] " + resposta);
            return new JSONObject(resposta);
        } catch (Exception e) {
            log("[Erro] " + e.getMessage() + ".");
            return null;
        }
    }

    private void login() {
        String usuario = campoLoginUsuario.getText().trim();
        String senha = new String(campoLoginSenha.getPassword());
        JSONObject pedido = new JSONObject()
            .put("op", "login")
            .put("usuario", usuario)
            .put("senha", senha);
        JSONObject r = enviarEReceber(pedido);
        
        if (r == null) return;
        
        if ("200".equals(r.optString("resposta"))) {
            tokenAtual = r.optString("token");
            lblToken.setText(tokenAtual);
            consultarUsuario();
            cardLayout.show(painelPrincipal, "usuario");
            JOptionPane.showMessageDialog(this, "Login efetuado com sucesso.\nToken: " + tokenAtual);
        } else {
            JOptionPane.showMessageDialog(this, r.optString("mensagem"), "Erro de Login", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cadastrar() {
        String nome = campoCadNome.getText().trim();
        String usuario = campoCadUsuario.getText().trim();
        String senha = new String(campoCadSenha.getPassword());
        JSONObject pedido = new JSONObject()
            .put("op", "cadastrarUsuario")
            .put("nome", nome)
            .put("usuario", usuario)
            .put("senha", senha);
        JSONObject r = enviarEReceber(pedido);
        
        if (r == null) return;
        
        if ("200".equals(r.optString("resposta"))) {
            JOptionPane.showMessageDialog(this, "Cadastrado com sucesso.");
            campoCadNome.setText("");
            campoCadUsuario.setText("");
            campoCadSenha.setText("");
        } else {
            JOptionPane.showMessageDialog(this, r.optString("mensagem"), "Erro no Cadastro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void consultarUsuario() {
        JSONObject pedido = new JSONObject()
            .put("op", "consultarUsuario")
            .put("token", tokenAtual);
        JSONObject r = enviarEReceber(pedido);
        
        if (r == null) return;
        
        if ("200".equals(r.optString("resposta"))) {
            lblNome.setText(r.optString("nome"));
            lblUsuario.setText(r.optString("usuario"));
        } else {
            JOptionPane.showMessageDialog(this, r.optString("mensagem"), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarUsuario() {
        String nome = campoAtuNome.getText().trim();
        String senha = new String(campoAtuSenha.getPassword());
        JSONObject pedido = new JSONObject()
            .put("op", "atualizarUsuario")
            .put("token", tokenAtual)
            .put("nome", nome)
            .put("senha", senha);
        JSONObject r = enviarEReceber(pedido);
        
        if (r == null) return;
        
        if ("200".equals(r.optString("resposta"))) {
            JOptionPane.showMessageDialog(this, "Dados atualizados com sucesso.");
            campoAtuNome.setText("");
            campoAtuSenha.setText("");
            consultarUsuario();
        } else {
            JOptionPane.showMessageDialog(this, r.optString("mensagem"), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletarUsuario() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Tem certeza que deseja deletar sua conta? Esta ação não pode ser desfeita.",
            "Confirmar Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        JSONObject pedido = new JSONObject()
            .put("op", "deletarUsuario")
            .put("token", tokenAtual);
        JSONObject r = enviarEReceber(pedido);
        
        if (r == null) return;
        
        if ("200".equals(r.optString("resposta"))) {
            JOptionPane.showMessageDialog(this, "Conta deletada com sucesso.");
            tokenAtual = null;
            lblNome.setText("-");
            lblUsuario.setText("-");
            lblToken.setText("-");
            cardLayout.show(painelPrincipal, "auth");
        } else {
            JOptionPane.showMessageDialog(this, r.optString("mensagem"), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        JSONObject pedido = new JSONObject()
            .put("op", "logout")
            .put("token", tokenAtual);
        JSONObject r = enviarEReceber(pedido);
        
        if (r == null) return;
        
        if ("200".equals(r.optString("resposta"))) {
            JOptionPane.showMessageDialog(this, "Logout efetuado com sucesso.");
        } else {
            JOptionPane.showMessageDialog(this, r.optString("mensagem"), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
        
        tokenAtual = null;
        lblNome.setText("-");
        lblUsuario.setText("-");
        lblToken.setText("-");
        cardLayout.show(painelPrincipal, "auth");
    }

    private void log(String msg) {
        String ts = LocalDateTime.now().format(FMT);
        SwingUtilities.invokeLater(() -> {
            areaLog.append("[" + ts + "] " + msg + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente().setVisible(true));
    }
}
