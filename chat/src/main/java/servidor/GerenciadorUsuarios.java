package servidor;

import comum.Usuario;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;

public class GerenciadorUsuarios {

    private final UsuarioDAO dao = new UsuarioDAO();

    public synchronized String cadastrar(String nome, String usuario, String senha) {
        String erroNome = validarNome(nome);
        if (erroNome != null) return erroNome;
        String erroUsuario = validarUsuario(usuario);
        if (erroUsuario != null) return erroUsuario;
        String erroSenha = validarSenha(senha);
        if (erroSenha != null) return erroSenha;

        try {
            String chave = usuario.toLowerCase();
            if (dao.existeUsuario(chave))
                return "Usuário '" + chave + "' já está cadastrado.";

            String token = "usr_" + chave;
            Usuario novo = new Usuario(nome.trim(), chave, senha, token);
            dao.inserir(novo);
            return null;
        } catch (SQLException e) {
            return mensagemBanco(e);
        }
    }

    public Usuario consultarPorToken(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            return dao.buscarPorToken(token);
        } catch (SQLException e) {
            return null;
        }
    }

    public synchronized String atualizar(String token, String novoNome, String novaSenha) {
        if (token == null || token.isEmpty())
            return "Token vazio: forneça um token válido.";
        String erroNome = validarNome(novoNome);
        if (erroNome != null) return erroNome;
        String erroSenha = validarSenha(novaSenha);
        if (erroSenha != null) return erroSenha;

        try {
            Usuario u = dao.buscarPorToken(token);
            if (u == null)
                return "Token inválido: não corresponde a nenhum usuário cadastrado.";
            dao.atualizar(token, novoNome.trim(), novaSenha);
            return null;
        } catch (SQLException e) {
            return mensagemBanco(e);
        }
    }

    public synchronized String deletar(String token) {
        if (token == null || token.isEmpty())
            return "Token vazio: forneça um token válido.";
        try {
            return dao.deletarPorToken(token)
                ? null
                : "Token inválido: não corresponde a nenhum usuário cadastrado.";
        } catch (SQLException e) {
            return mensagemBanco(e);
        }
    }

    public String login(String usuario, String senha) {
        if (usuario == null || senha == null) return null;
        if (usuario.isEmpty() || senha.isEmpty()) return null;
        try {
            Usuario u = dao.buscarPorUsuario(usuario.toLowerCase());
            if (u == null || !u.getSenha().equals(senha)) return null;
            return u.getToken();
        } catch (SQLException e) {
            return null;
        }
    }

    public boolean logout(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            return dao.buscarPorToken(token) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    private String validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty())
            return "Nome não pode ser vazio.";
        if (nome.trim().length() > 255)
            return "Nome muito longo: máximo 255 caracteres (recebido: " + nome.trim().length() + ").";
        return null;
    }

    private String validarUsuario(String usuario) {
        if (usuario == null || usuario.isEmpty())
            return "Usuário não pode ser vazio.";
        if (usuario.length() < 5)
            return "Usuário muito curto: mínimo 5 caracteres (recebido: " + usuario.length() + ").";
        if (usuario.length() > 20)
            return "Usuário muito longo: máximo 20 caracteres (recebido: " + usuario.length() + ").";
        if (!usuario.matches("[a-zA-Z0-9]+"))
            return "Usuário inválido: use apenas letras e números (sem espaços, acentos ou caracteres especiais).";
        return null;
    }

    private String validarSenha(String senha) {
        if (senha == null || senha.isEmpty())
            return "Senha não pode ser vazia.";
        if (senha.length() != 6)
            return "Senha deve ter exatamente 6 dígitos (recebida com " + senha.length() + ").";
        if (!senha.matches("[0-9]+"))
            return "Senha inválida: use apenas dígitos numéricos (0-9).";
        return null;
    }

    private String mensagemBanco(SQLException e) {
        if (e instanceof SQLNonTransientConnectionException
            || e instanceof SQLTransientConnectionException) {
            return "Banco de dados indisponível no momento. Tente novamente em alguns instantes.";
        }
        return "Erro no banco de dados: " + e.getMessage() + ".";
    }
}
