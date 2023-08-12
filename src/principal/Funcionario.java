package principal;

public class Funcionario {
    private String cpf;
    private String nome;
    private String login_acesso;
    private String senha;
    private String cargo;

    public Funcionario(String cpf, String nome, String login_acesso, String senha, String cargo) {
        this.cpf = cpf;
        this.nome = nome;
        this.login_acesso = login_acesso;
        this.senha = senha;
        this.cargo = cargo;
    }

    public Funcionario() {
        this.cpf = "";
        this.nome = "";
        this.login_acesso = "";
        this.senha = "";
        this.cargo = "";
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin_acesso() {
        return login_acesso;
    }

    public void setLogin_acesso(String login_acesso) {
        this.login_acesso = login_acesso;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public static Funcionario parseFuncionario(String linha) {
        String campos[] = linha.split(";");
        if (campos.length >= 5) {
            Funcionario aux = new Funcionario();
            aux.setCpf(campos[0]);
            aux.setNome(campos[1]);
            aux.setLogin_acesso(campos[2]);
            aux.setSenha(campos[3]);
            aux.setCargo(campos[4]);
            return aux;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return this.cpf + ";" + this.nome + ";" + this.login_acesso +
                ";" + this.senha + ";" + this.cargo;
    }

}
