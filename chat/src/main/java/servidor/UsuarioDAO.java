package servidor;

import comum.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    public boolean existeUsuario(String usuario) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE usuario = ?";
        try (Connection c = ConexaoBD.abrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void inserir(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuarios (usuario, nome, senha, token) VALUES (?, ?, ?, ?)";
        try (Connection c = ConexaoBD.abrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsuario());
            ps.setString(2, u.getNome());
            ps.setString(3, u.getSenha());
            ps.setString(4, u.getToken());
            ps.executeUpdate();
        }
    }

    public Usuario buscarPorUsuario(String usuario) throws SQLException {
        String sql = "SELECT usuario, nome, senha, token FROM usuarios WHERE usuario = ?";
        try (Connection c = ConexaoBD.abrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Usuario buscarPorToken(String token) throws SQLException {
        String sql = "SELECT usuario, nome, senha, token FROM usuarios WHERE token = ?";
        try (Connection c = ConexaoBD.abrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void atualizar(String token, String novoNome, String novaSenha) throws SQLException {
        String sql = "UPDATE usuarios SET nome = ?, senha = ? WHERE token = ?";
        try (Connection c = ConexaoBD.abrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setString(2, novaSenha);
            ps.setString(3, token);
            ps.executeUpdate();
        }
    }

    public boolean deletarPorToken(String token) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE token = ?";
        try (Connection c = ConexaoBD.abrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            return ps.executeUpdate() > 0;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
            rs.getString("nome"),
            rs.getString("usuario"),
            rs.getString("senha"),
            rs.getString("token")
        );
    }
}
