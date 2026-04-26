package servidor;

import comum.Usuario;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Consumer;

public class ClienteHandler implements Runnable {

    private static final int LIMITE_LINHA = 8192;

    private static final Set<String> OPERACOES_VALIDAS = Set.of(
        "login", "logout",
        "cadastrarUsuario", "consultarUsuario", "atualizarUsuario", "deletarUsuario"
    );

    private final Socket socket;
    private final GerenciadorUsuarios gerenciador;
    private final Consumer<String> log;
    private final Set<Socket> conexoesAtivas;

    public ClienteHandler(Socket socket, GerenciadorUsuarios gerenciador,
                          Consumer<String> log, Set<Socket> conexoesAtivas) {
        this.socket = socket;
        this.gerenciador = gerenciador;
        this.log = log;
        this.conexoesAtivas = conexoesAtivas;
    }

    @Override
    public void run() {
        String endereco = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        conexoesAtivas.add(socket);
        log.accept("[Conexão] Cliente conectado: " + endereco + ".");

        CharsetDecoder dec = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), dec));
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            while (true) {
                String linha;
                try {
                    linha = readLineLimitado(in);
                } catch (LinhaMuitoGrandeException e) {
                    String msg = erro("Mensagem excede o tamanho máximo permitido de "
                        + LIMITE_LINHA + " bytes. Conexão será encerrada.");
                    out.println(msg);
                    log.accept("[Bloqueado] " + endereco + ": linha excedeu " + LIMITE_LINHA + " bytes.");
                    log.accept("[Enviado para " + endereco + "] " + msg);
                    break;
                } catch (CharacterCodingException e) {
                    String msg = erro("Encoding inválido: a mensagem deve estar em UTF-8. "
                        + "Conexão será encerrada.");
                    out.println(msg);
                    log.accept("[Bloqueado] " + endereco + ": bytes não decodificáveis em UTF-8.");
                    log.accept("[Enviado para " + endereco + "] " + msg);
                    break;
                }

                if (linha == null) break;

                log.accept("[Recebido de " + endereco + "] " + linha);
                String resposta = processar(linha);
                out.println(resposta);
                log.accept("[Enviado para " + endereco + "] " + resposta);
            }
        } catch (IOException e) {
            log.accept("[Erro de I/O] " + endereco + ": " + e.getMessage() + ".");
        } finally {
            conexoesAtivas.remove(socket);
            log.accept("[Conexão] Cliente desconectado: " + endereco + ".");
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private String readLineLimitado(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') return sb.toString();
            if (c == '\r') continue;
            sb.append((char) c);
            if (sb.length() > LIMITE_LINHA) {
                throw new LinhaMuitoGrandeException();
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static class LinhaMuitoGrandeException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    private String processar(String json) {
        try {
            JSONObject msg;
            try {
                msg = new JSONObject(json);
            } catch (JSONException e) {
                return erro("JSON malformado: " + e.getMessage() + ".");
            }

            if (!msg.has("op")) {
                return erro("Campo 'op' ausente. Indique a operação desejada.");
            }
            Object opVal = msg.opt("op");
            if (opVal == null || JSONObject.NULL.equals(opVal)) {
                return erro("Campo 'op' nulo. Indique uma operação válida.");
            }
            String op = msg.optString("op", "").trim();
            if (op.isEmpty()) {
                return erro("Campo 'op' vazio. Indique uma operação válida.");
            }
            if (!OPERACOES_VALIDAS.contains(op)) {
                return erro("Operação desconhecida: '" + op + "'. Operações válidas: "
                    + String.join(", ", OPERACOES_VALIDAS) + ".");
            }

            switch (op) {
                case "login":            return processarLogin(msg);
                case "logout":           return processarLogout(msg);
                case "cadastrarUsuario": return processarCadastrar(msg);
                case "consultarUsuario": return processarConsultar(msg);
                case "atualizarUsuario": return processarAtualizar(msg);
                case "deletarUsuario":   return processarDeletar(msg);
                default:                 return erro("Operação desconhecida: '" + op + "'.");
            }
        } catch (Throwable t) {
            log.accept("[Erro interno] " + t.getClass().getSimpleName() + ": " + t.getMessage() + ".");
            return erro("Erro interno no servidor (" + t.getClass().getSimpleName() + "). "
                + "Verifique o formato da sua mensagem.");
        }
    }

    private String validarCampo(JSONObject msg, String campo) {
        if (!msg.has(campo)) return "Campo '" + campo + "' ausente.";
        Object val = msg.opt(campo);
        if (val == null || JSONObject.NULL.equals(val)) return "Campo '" + campo + "' nulo.";
        if (!(val instanceof String)) {
            return "Campo '" + campo + "' deve ser string (recebido: "
                + val.getClass().getSimpleName() + ").";
        }
        return null;
    }

    private String processarLogin(JSONObject msg) {
        String e1 = validarCampo(msg, "usuario"); if (e1 != null) return erro(e1);
        String e2 = validarCampo(msg, "senha");   if (e2 != null) return erro(e2);

        String usuario = msg.optString("usuario", "");
        String senha = msg.optString("senha", "");
        String token = gerenciador.login(usuario, senha);
        if (token != null) {
            return new JSONObject()
                .put("resposta", "200")
                .put("token", token)
                .toString();
        }
        return erro("Usuário ou senha inválidos.");
    }

    private String processarLogout(JSONObject msg) {
        String e = validarCampo(msg, "token"); if (e != null) return erro(e);
        String token = msg.optString("token", "");
        if (gerenciador.logout(token)) {
            return new JSONObject()
                .put("resposta", "200")
                .put("mensagem", "Logout efetuado.")
                .toString();
        }
        return erro("Erro ao efetuar logout: token não corresponde a nenhum usuário.");
    }

    private String processarCadastrar(JSONObject msg) {
        String e1 = validarCampo(msg, "nome");    if (e1 != null) return erro(e1);
        String e2 = validarCampo(msg, "usuario"); if (e2 != null) return erro(e2);
        String e3 = validarCampo(msg, "senha");   if (e3 != null) return erro(e3);

        String nome = msg.optString("nome", "");
        String usuario = msg.optString("usuario", "");
        String senha = msg.optString("senha", "");
        String erroNeg = gerenciador.cadastrar(nome, usuario, senha);
        if (erroNeg == null) {
            return new JSONObject()
                .put("resposta", "200")
                .put("mensagem", "Cadastrado com sucesso.")
                .toString();
        }
        return erro(erroNeg);
    }

    private String processarConsultar(JSONObject msg) {
        String e = validarCampo(msg, "token"); if (e != null) return erro(e);
        String token = msg.optString("token", "");
        Usuario u = gerenciador.consultarPorToken(token);
        if (u != null) {
            return new JSONObject()
                .put("resposta", "200")
                .put("nome", u.getNome())
                .put("usuario", u.getUsuario())
                .toString();
        }
        return erro("Token inválido: não corresponde a nenhum usuário cadastrado.");
    }

    private String processarAtualizar(JSONObject msg) {
        String e1 = validarCampo(msg, "token"); if (e1 != null) return erro(e1);
        String e2 = validarCampo(msg, "nome");  if (e2 != null) return erro(e2);
        String e3 = validarCampo(msg, "senha"); if (e3 != null) return erro(e3);

        String token = msg.optString("token", "");
        String nome = msg.optString("nome", "");
        String senha = msg.optString("senha", "");
        String erroNeg = gerenciador.atualizar(token, nome, senha);
        if (erroNeg == null) {
            return new JSONObject()
                .put("resposta", "200")
                .put("mensagem", "Atualizado com sucesso.")
                .toString();
        }
        return erro(erroNeg);
    }

    private String processarDeletar(JSONObject msg) {
        String e = validarCampo(msg, "token"); if (e != null) return erro(e);
        String token = msg.optString("token", "");
        String erroNeg = gerenciador.deletar(token);
        if (erroNeg == null) {
            return new JSONObject()
                .put("resposta", "200")
                .put("mensagem", "Deletado com sucesso.")
                .toString();
        }
        return erro(erroNeg);
    }

    private static String erro(String mensagem) {
        return new JSONObject()
            .put("resposta", "401")
            .put("mensagem", mensagem)
            .toString();
    }
}
