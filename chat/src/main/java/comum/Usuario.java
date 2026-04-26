package comum;

public class Usuario {
    private String nome;
    private String usuario;
    private String senha;
    private String token;

    public Usuario(String nome, String usuario, String senha, String token) {
        this.nome = nome;
        this.usuario = usuario;
        this.senha = senha;
        this.token = token;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getUsuario() { return usuario; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getToken() { return token; }
}
