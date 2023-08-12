package principal;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import db.DB;
import db.DbException;
import db.DbIntegrityException;

import auxiliar.Criptografia;

public class Controlador extends JFrame {

    // Variaveis geradas pelo Chat GPT
    private static final String LOGIN_FILE = "login.csv";
    private static final String DATA_FILE_STRING = common.Paths.getDataPath();
    private static final String FUNC_DATA_FILE_STRING = common.Paths.getDataEmployeePath();
    private List<Cadaver> cadaveres;
    private List<Funcionario> funcionarios;
    private JPanel mainPanel;
    private JPanel homePanel;
    private JPanel optionsPanel;
    private CardLayout cardLayout;
    private static String nomeUsuarioLogado = "";
    private Connection conn;

    public Controlador(Connection conn) {

        this.conn = conn;

        setTitle("Gerenciamento Necroterio - User: " + nomeUsuarioLogado);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Esse array list recebe todos os registros do arquivo quando o programa é
        // iniciado
        cadaveres = new ArrayList<>();
        funcionarios = new ArrayList<>();

        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        // Chamada da funçao para criar painel home
        createHomePanel();

        mainPanel.add(homePanel, "home");

        add(mainPanel);

        // Verificar se existe um ADMIN cadastrado
        AdminCheck();

        if (realizarLogin()) {
            Inicializar(); // Verifica os arquivos de dados
            lerRegistrosDoArquivo(); // Essa funçao usa a variavel List<Cadaver> cadaveres
            showHomePage(); // Exibe a home page
            setTitle("Gerenciamento Necroterio - Usuário: " + nomeUsuarioLogado);
            setVisible(true);

        } else {

            JOptionPane.showMessageDialog(this, "Login ou Senha invalidos. Encerrendo o programa.");
            System.exit(0);

        }
    }

    /*
     * Metodo para verificar se ja existe o arquivo de admins, na pasta do projeto,
     * ../Novo/login.csv
     * caso não exista o metodo cria o arquivo e pede um admin inicial
     */
    private void AdminCheck() {
        File login = new File(LOGIN_FILE);

        if (!login.exists()) {
            try {
                if (login.createNewFile()) {
                    JOptionPane.showMessageDialog(this, "Criando primeiro admin...");
                    String nome = JOptionPane.showInputDialog(this, "Digite seu login:");
                    if (nome != null && !nome.isEmpty()) {
                        String senha = JOptionPane.showInputDialog(this, "Digite sua senha:");
                        if (senha != null && !senha.isEmpty()) {
                            String key = Criptografia.generateGUID();
                            try (FileWriter writer = new FileWriter(LOGIN_FILE, true)) {
                                writer.write(nome + ";" + Criptografia.encrypt(senha, key) + ";" + key);
                                writer.write("\n");
                                writer.flush();
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(this, "Error writing admin to file: " + e.getMessage());
                            }
                        }
                    }

                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error writing admin to file: " + e.getMessage());
            }

        }
    }

    /*
     * Metodo de inicialização para verificar se existe a pasta e o arquivo de dados
     * dos cadaveres
     * ambos sao criados no Desktop
     */
    private void Inicializar() {
        String folderPath = common.Paths.getDataFolderPath();
        File folder = new File(folderPath);
        String dataPath = folderPath + "/datas.csv";
        String dataEmployeePath = folderPath + "/dataemployee.csv";
        File data = new File(dataPath);
        File dataEmployee = new File(dataEmployeePath);

        if (!folder.exists()) {
            try {
                if (folder.mkdir()) {
                    JOptionPane.showMessageDialog(this, "Criando pasta...");
                } else {
                    JOptionPane.showMessageDialog(this, "Erro ao criar pasta...");
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }

        if (!data.exists()) {
            try {
                if (data.createNewFile()) {
                    JOptionPane.showMessageDialog(this, "Criando arquivo...");
                } else {
                    JOptionPane.showMessageDialog(this, "Erro ao criar o arquivo...");
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }

        if (!dataEmployee.exists()) {
            try {
                if (dataEmployee.createNewFile()) {
                    JOptionPane.showMessageDialog(this, "Criando arquivo de funcionários...");
                } else {
                    JOptionPane.showMessageDialog(this, "Erro ao criar o arquivo de funcionários...");
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    /* Metodo para realizar Login */
    private boolean realizarLogin() {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Bem vindo ao Gerenciamento do Necroterio");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        loginPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(2, 2));

        JLabel loginLabel = new JLabel("Login:");
        JTextField loginField = new JTextField();
        formPanel.add(loginLabel);
        formPanel.add(loginField);

        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        loginPanel.add(formPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(null, loginPanel, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String login = loginField.getText();
            String password = new String(passwordField.getPassword());

            try {
                List<String> credentials = Files.readAllLines(Paths.get(LOGIN_FILE));
                for (String line : credentials) {
                    String[] fields = line.split(";");
                    if (autenticarCriptografia(login, password)) {
                        nomeUsuarioLogado = login;
                        return true;
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading the login file: " + e.getMessage());
            }
        }

        return false;
    }

    private boolean autenticarAdmin() {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Autenticação de Administrador");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        loginPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(2, 2));

        JLabel loginLabel = new JLabel("Login:");
        JTextField loginField = new JTextField();
        formPanel.add(loginLabel);
        formPanel.add(loginField);

        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        loginPanel.add(formPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(null, loginPanel, "Autenticação de Administrador",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String login = loginField.getText();
            String password = new String(passwordField.getPassword());

            try {
                List<String> adminCredentials = Files.readAllLines(Paths.get(LOGIN_FILE));
                for (String line : adminCredentials) {
                    String[] fields = line.split(";");
                    if (autenticarCriptografia(login, password)) {
                        JOptionPane.showMessageDialog(this, "Sucesso...");
                        return true;
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading the admin credentials file: " + e.getMessage());
            }
        }
        JOptionPane.showMessageDialog(this, "Falha...");
        return false;
    }

    private boolean autenticarCriptografia(String login, String senha) {
        try {
            List<String> adminCredentials = Files.readAllLines(Paths.get(LOGIN_FILE));
            for (String line : adminCredentials) {
                String[] fields = line.split(";");
                if (fields.length == 3 && fields[0].equals(login)) {
                    String storedEncryptedPassword = fields[1];
                    String storedKey = fields[2];
                    String encryptedPassword = "";
                    try {
                        encryptedPassword = Criptografia.encrypt(senha, storedKey);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
                    }

                    if (storedEncryptedPassword.equals(encryptedPassword)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading the admin credentials file: " + e.getMessage());
        }
        return false;
    }

    private void createHomePanel() {

        homePanel = new JPanel();
        homePanel.setLayout(new BorderLayout());

        // Create the bottom panel for the "Área do Administrador" button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton adminButton = new JButton("Área do Administrador");
        adminButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (autenticarAdmin()) {
                    abrirOpcoesAdmin();
                }
            }
        });
        bottomPanel.add(adminButton);
        // Add the bottom panel to the page panel
        homePanel.add(bottomPanel, BorderLayout.SOUTH);

        // Create the center panel for the main buttons
        JPanel centerPanel = new JPanel(new GridLayout(0, 3, 20, 40)); // 3 columns, variable rows, 10px vertical and
                                                                       // horizontal gaps

        // Create botão Listar Registros
        JButton produtosButton = new JButton("Lista de Registros");
        produtosButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mostrarCadaver();
            }
        });
        centerPanel.add(produtosButton);

        // Criar botão Adicionar Registro
        JButton addProdutoButton = new JButton("Adicionar Registro");
        addProdutoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                adicionarCadaver();
            }
        });
        centerPanel.add(addProdutoButton);

        // Criando botão atualizar cadaver
        JButton alterarButton = new JButton("Alterar Registro");
        alterarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                atualizarRegistroPorCPF();
            }
        });
        centerPanel.add(alterarButton);

        // Novo botão Buscar cadaver
        JButton buscarButton = new JButton("Buscar Registro");
        buscarButton.setPreferredSize(new Dimension(50, 10));
        buscarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buscarRegistros();
            }
        });
        centerPanel.add(buscarButton);

        // Novo botão Apagar cadaver
        JButton apagarButton = new JButton("Apagar Registro");
        apagarButton.setPreferredSize(new Dimension(50, 10));
        apagarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                apagarRegistroPorCPF();
            }
        });
        centerPanel.add(apagarButton);

        // Novo botão Situação cadaver
        JButton situacaoButton = new JButton("Alterar Situação");
        situacaoButton.setPreferredSize(new Dimension(50, 10));
        situacaoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                alterarSituacaoCadaver();
            }
        });
        centerPanel.add(situacaoButton);

        // Novo botão Buscar cadaver
        JButton ordenarButton = new JButton("Ordenar Registros");
        ordenarButton.setPreferredSize(new Dimension(50, 10));
        ordenarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listaOrdenada();
            }
        });
        centerPanel.add(ordenarButton);

        // Novo botão Procedimento no cadaver
        JButton procedimentoButton = new JButton("Adicionar Procedimento");
        procedimentoButton.setPreferredSize(new Dimension(50, 10));
        procedimentoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EmDesenvolvimento();
            }
        });
        centerPanel.add(procedimentoButton);

        // Botão para Listar de Funcionarios
        JButton funcionariosButton = new JButton("Lista de Funcionarios");
        funcionariosButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mostrarFuncionario();
            }
        });
        centerPanel.add(funcionariosButton);

        // Criar botão Adicionar Registro
        JButton addFuncionarioButton = new JButton("Adicionar Funcionario");
        addFuncionarioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                adicionarFuncionario();
            }
        });
        centerPanel.add(addFuncionarioButton);

        // Novo botão Encerrar Sistema
        JButton encerrarButton = new JButton("Encerrar Sessão");
        encerrarButton.setPreferredSize(new Dimension(50, 10));
        encerrarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sairDoSistema();
            }
        });
        centerPanel.add(encerrarButton);

        // Add the center panel to the page panel
        homePanel.add(centerPanel, BorderLayout.CENTER);
    }

    private void mostrarFuncionario() {
        lerFuncionariosDoArquivo(); // Read the products from the file

        // Create the table model
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(
                new Object[] { "CPF", "Nome", "Login", "Senha", "Cargo" });

        // Populate the table model with data from the cadaver list
        for (Funcionario funcionario : funcionarios) {
            tableModel.addRow(new Object[] { funcionario.getCpf(), funcionario.getNome(), funcionario.getLogin_acesso(),
                    funcionario.getSenha(), funcionario.getCargo() });
        }

        // Create the table and scroll pane
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Create the dialog and display the table
        JDialog dialog = new JDialog(this, "Funcionarios", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(scrollPane);
        dialog.pack();

        // Set the size of the dialog
        dialog.setSize(900, 500);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /*
     * Metodo executado quando clicado no botao LISTAR REGISTROS
     * abre uma nova tela e exibe todos os registros do arquivo
     */
    private void mostrarCadaver() {
        lerRegistrosDoArquivo(); // Read the products from the file

        // Create the table model
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(
                new Object[] { "CPF", "Nome", "Peso", "Data da morte", "Hora da morte", "Situação" });

        // Populate the table model with data from the cadaver list
        for (Cadaver cadaver : cadaveres) {
            tableModel.addRow(new Object[] { cadaver.getCpf(), cadaver.getNome(), cadaver.getPeso(),
                    cadaver.getDataFalecimento(), cadaver.getHoraFalecimento(), cadaver.getSituacao() });
        }

        // Create the table and scroll pane
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Create the dialog and display the table
        JDialog dialog = new JDialog(this, "Cadaveres", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(scrollPane);
        dialog.pack();

        // Set the size of the dialog
        dialog.setSize(900, 500);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void mostrarCadaverOrdenado() {

        // Create the table model
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(
                new Object[] { "CPF", "Nome", "Peso", "Data da morte", "Hora da morte", "Situação" });

        // Populate the table model with data from the cadaver list
        for (Cadaver cadaver : cadaveres) {
            tableModel.addRow(new Object[] { cadaver.getCpf(), cadaver.getNome(), cadaver.getPeso(),
                    cadaver.getDataFalecimento(), cadaver.getHoraFalecimento(), cadaver.getSituacao() });
        }

        // Create the table and scroll pane
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Create the dialog and display the table
        JDialog dialog = new JDialog(this, "Cadaveres", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(scrollPane);
        dialog.pack();

        // Set the size of the dialog
        dialog.setSize(900, 500);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void mostrarCadaverOrdenado(List<Cadaver> searchResults) {

        // Create the table model
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(
                new Object[] { "CPF", "Nome", "Peso", "Data da morte", "Hora da morte", "Situação" });

        // Populate the table model with data from the cadaver list
        for (Cadaver cadaver : searchResults) {
            tableModel.addRow(new Object[] { cadaver.getCpf(), cadaver.getNome(), cadaver.getPeso(),
                    cadaver.getDataFalecimento(), cadaver.getHoraFalecimento(), cadaver.getSituacao() });
        }

        // Create the table and scroll pane
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Create the dialog and display the table
        JDialog dialog = new JDialog(this, "Cadaveres", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(scrollPane);
        dialog.pack();

        // Set the size of the dialog
        dialog.setSize(900, 500);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /* Metodo apenas para EXIBIR a pagina inicial */
    private void showHomePage() {
        cardLayout.show(mainPanel, "home");
    }

    private void adicionarFuncionario() {
        JTextField cpfField = new JTextField(15);
        JTextField nomeField = new JTextField(15);
        JTextField login_acessoField = new JTextField(15);
        JTextField senhaField = new JTextField(15);
        JTextField cargoField = new JTextField(15);

        // Set placeholder for CPF field
        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cpfField.getText().equals("Apenas Números")) {
                    cpfField.setText("");
                    cpfField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (cpfField.getText().isEmpty()) {
                    cpfField.setText("Apenas Números");
                    cpfField.setForeground(Color.GRAY);
                }
            }
        });

        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("CPF:"));
        panel.add(cpfField);
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Login:"));
        panel.add(login_acessoField);
        panel.add(new JLabel("Senha:"));
        panel.add(senhaField);
        panel.add(new JLabel("Cargo:"));
        panel.add(cargoField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Adicionar Funcionário", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String cpf = cpfField.getText().replaceAll("[^0-9]", ""); // Remove non-numeric characters from the input
            String nome = nomeField.getText();
            String login_acesso = login_acessoField.getText();
            String senha = senhaField.getText();
            String cargo = cargoField.getText();

            if (confirmarCampos(cpf, nome, login_acesso, login_acesso, cargo)) {
                // Show a confirmation dialog before adding the record
                String message = "Deseja adicionar o seguinte funcionário?\n\n"
                        + "CPF: " + formatCPF(cpf) + "\n"
                        + "Nome: " + nome + "\n"
                        + "Login: " + login_acesso + "\n"
                        + "Senha: " + senha + "\n"
                        + "Cargo: " + cargo + "\n";

                int confirmation = JOptionPane.showConfirmDialog(this, message, "Confirmação",
                        JOptionPane.YES_NO_OPTION);

                if (confirmation == JOptionPane.YES_OPTION) {
                    Funcionario funcionario = new Funcionario(formatCPF(cpf), nome, login_acesso, senha, cargo);
                    funcionarios.add(funcionario);
                    escreverFuncionarioNoArquivo();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Preencha todos os campos antes de adicionar o funcionário.");
            }
        }
    }

    /*
     * Metodo executado quando clicado no botao ADICIONAR REGISTROS
     * abre varias caixas de dialogo e adiciona um novo registro no arquivo
     */
    private void adicionarCadaver() {
        JTextField cpfField = new JTextField(15);
        JTextField nomeField = new JTextField(15);
        JTextField pesoField = new JTextField(15);
        JTextField dataFalecimentoField = new JTextField(15);
        JTextField horaFalecimentoField = new JTextField(15);

        // Set placeholder for CPF field
        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cpfField.getText().equals("Apenas Números")) {
                    cpfField.setText("");
                    cpfField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (cpfField.getText().isEmpty()) {
                    cpfField.setText("Apenas Números");
                    cpfField.setForeground(Color.GRAY);
                }
            }
        });
        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        // Set placeholder for Data de Falacimento field
        dataFalecimentoField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (dataFalecimentoField.getText().equals("DD/MM/YYYY")) {
                    dataFalecimentoField.setText("");
                    dataFalecimentoField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (dataFalecimentoField.getText().isEmpty()) {
                    dataFalecimentoField.setText("DD/MM/YYYY");
                    dataFalecimentoField.setForeground(Color.GRAY);
                }
            }
        });
        dataFalecimentoField.setText("DD/MM/YYYY");
        dataFalecimentoField.setForeground(Color.GRAY);

        // Set placeholder for Hora de Falacimento field
        horaFalecimentoField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (horaFalecimentoField.getText().equals("HH:MM")) {
                    horaFalecimentoField.setText("");
                    horaFalecimentoField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (horaFalecimentoField.getText().isEmpty()) {
                    horaFalecimentoField.setText("HH:MM");
                    horaFalecimentoField.setForeground(Color.GRAY);
                }
            }
        });
        horaFalecimentoField.setText("HH:MM");
        horaFalecimentoField.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("CPF:"));
        panel.add(cpfField);
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Peso:"));
        panel.add(pesoField);
        panel.add(new JLabel("Data da Morte:"));
        panel.add(dataFalecimentoField);
        panel.add(new JLabel("Hora da Morte:"));
        panel.add(horaFalecimentoField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Adicionar Cadáver", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String cpf = cpfField.getText().replaceAll("[^0-9]", ""); // Remove non-numeric characters from the input
            String nome = nomeField.getText();
            double peso = Double.parseDouble(pesoField.getText());
            String dataFalecimento = dataFalecimentoField.getText();
            String horaFalecimento = horaFalecimentoField.getText();

            if (confirmarEntrada(cpf, nome, peso, dataFalecimento, horaFalecimento)) {
                // Show a confirmation dialog before adding the record
                String message = "Deseja adicionar o seguinte cadáver?\n\n"
                        + "CPF: " + formatCPF(cpf) + "\n"
                        + "Nome: " + nome + "\n"
                        + "Peso: " + peso + "\n"
                        + "Data da Morte: " + dataFalecimento + "\n"
                        + "Hora da Morte: " + horaFalecimento + "\n";

                int confirmation = JOptionPane.showConfirmDialog(this, message, "Confirmação",
                        JOptionPane.YES_NO_OPTION);

                if (confirmation == JOptionPane.YES_OPTION) {
                    Cadaver corpo = new Cadaver(formatCPF(cpf), nome, dataFalecimento, horaFalecimento, peso);

                    // ---------- CONSULTA BANCO DE DADOS ---------------------
                    PreparedStatement st = null;
                    try {
                        st = conn.prepareStatement(
                                "INSERT INTO cadaver " +
                                        "(identificacao, nome_cadaver, peso, dataMorte, horaMorte, situacao) " +
                                        "VALUES " +
                                        "(?, ?, ?, ?, ?, ?)",
                                Statement.RETURN_GENERATED_KEYS);

                        st.setString(1, corpo.getCpf());
                        st.setString(2, corpo.getNome());
                        st.setDouble(3, corpo.getPeso());
                        st.setString(4, corpo.getDataFalecimento());
                        st.setString(5, corpo.getHoraFalecimento());
                        st.setString(6, corpo.getSituacao());

                        int rowsAffected = st.executeUpdate();

                        if (rowsAffected > 0) {
                            System.out.println("Contato inserido\n");
                        } else {
                            throw new DbException("Erro ao inserir!");
                        }
                    } catch (SQLException e) {
                        throw new DbException(e.getMessage());
                    } finally {
                        DB.closeStatement(st);
                    }
                }
            }
        }

    }

    /*--------Metodo para ATUALIZAR cadáver--------*/
    public void atualizarRegistroPorCPF() {
        JTextField cpfField = new JTextField(15);

        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cpfField.getText().equals("Apenas Números")) {
                    cpfField.setText("");
                    cpfField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (cpfField.getText().isEmpty()) {
                    cpfField.setText("Apenas Números");
                    cpfField.setForeground(Color.GRAY);
                }
            }
        });
        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Digite o CPF para alterar dados do registro:"));
        panel.add(cpfField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Digite o CPF", JOptionPane.OK_CANCEL_OPTION);

        String searchQuery = cpfField.getText();

        if (searchQuery == null || searchQuery.equals("Apenas Números")) {
            return;
        }
        if (result == JOptionPane.OK_OPTION) {
            searchQuery = searchQuery.replaceAll("[^0-9]", "");
            searchQuery = formatCPF(searchQuery);

            PreparedStatement st = null;
            ResultSet rs = null;
            // Aqui retorna todos os dados do cpf que buscou
            try {
                st = conn.prepareStatement(
                        "SELECT * FROM cadaver WHERE identificacao = ?");
                st.setString(1, searchQuery);

                rs = st.executeQuery();
                if (rs.next()) {
                    String updatedName = JOptionPane.showInputDialog(this, "Digite o novo nome:",
                            rs.getString("nome_cadaver"));
                    String updatedWeight = JOptionPane.showInputDialog(this, "Digite o novo peso:",
                            rs.getDouble("peso"));
                    String updatedDeathDate = JOptionPane.showInputDialog(this, "Digite a nova data de óbito:",
                            rs.getString("dataMorte"));
                    String updatedTimeDate = JOptionPane.showInputDialog(this, "Digite a nova hora de óbito:",
                            rs.getString("horaMorte"));
                    if (updatedName != null && !updatedName.isEmpty() && updatedWeight != null
                            && !updatedWeight.isEmpty() && updatedDeathDate != null && !updatedDeathDate.isEmpty()
                            && updatedTimeDate != null && !updatedTimeDate.isEmpty()) {
                        double updatedWeightD = Double.parseDouble(updatedWeight);
                        // ---------- CONSULTA BANCO DE DADOS ---------------------
                        // Aqui atualiza todos os dados do cpf inserido
                        try {
                            st = conn.prepareStatement(

                                    "UPDATE cadaver SET nome_cadaver = ?, peso = ?, dataMorte = ?, horaMorte = ?  WHERE identificacao = ?",
                                    Statement.RETURN_GENERATED_KEYS);

                            st.setString(1, updatedName);
                            st.setDouble(2, updatedWeightD);
                            st.setString(3, updatedDeathDate);
                            st.setString(4, updatedTimeDate);
                            st.setString(5, searchQuery);

                            int rowsAffected = st.executeUpdate();

                            if (rowsAffected > 0) {
                                System.out.println("Registro atualizado!\n");
                            } else {
                                throw new DbException("Erro ao inserir!");
                            }
                        } catch (SQLException e) {
                            throw new DbException(e.getMessage());
                        } finally {
                            DB.closeStatement(st);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Os registros não podem ser vazios.");
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "CPF não encontrado.");
            }
        }
    }

    /*--------FIM do metodo para ATUALIZAR cadáver--------*/

    private String formatCPF(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private boolean confirmarCampos(String cpf, String nome, String login_acesso, String senha, String cargo) {
        if (cpf.length() < 10 || cpf.isEmpty()) {
            return false;
        }

        if (nome.isEmpty() && login_acesso.isEmpty() && senha.isEmpty() && cargo.isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean confirmarEntrada(String cpf, String nome, Double peso, String dataFalecimento,
            String horaFalecimento) {
        if (cpf.length() < 10 || cpf.isEmpty()) {
            return false;
        }

        if (nome.isEmpty() && peso == 0 && dataFalecimento.isEmpty() && horaFalecimento.isEmpty()) {
            return false;
        }

        return true;
    }

    /* Metodo auxiliar para escrever dados no arquivo .csv */
    private void escreverRegistrosNoArquivo() {
        try (FileWriter writer = new FileWriter(DATA_FILE_STRING, true)) {
            Cadaver corpo = cadaveres.get(cadaveres.size() - 1);
            writer.write(corpo.toString());
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error writing to produtos file: " + e.getMessage());
        }
    }

    void escreverFuncionarioNoArquivo() {
        try (FileWriter writer = new FileWriter(FUNC_DATA_FILE_STRING, true)) {
            Funcionario funcionario = funcionarios.get(funcionarios.size() - 1);
            writer.write(funcionario.toString());
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error writing to produtos file: " + e.getMessage());
        }
    }

    /* Metodo auxiliar para ler todos os registros do arquivo .csv */
    public void lerRegistrosDoArquivo() {
        cadaveres.clear();
        try {
            List<String> linhas = Files.readAllLines(Paths.get(DATA_FILE_STRING));
            for (String linha : linhas) {
                Cadaver corpo = Cadaver.parseCadaver(linha);
                cadaveres.add(corpo);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading produtos file: " + e.getMessage());
        }
    }

    public void lerFuncionariosDoArquivo() {
        funcionarios.clear();
        try {
            List<String> linhas = Files.readAllLines(Paths.get(FUNC_DATA_FILE_STRING));
            for (String linha : linhas) {
                Funcionario funcionario = Funcionario.parseFuncionario(linha);
                funcionarios.add(funcionario);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading produtos file: " + e.getMessage());
        }
    }

    private void EmDesenvolvimento() {
        JOptionPane.showMessageDialog(this, "Em Desenvolvimento...");
    }

    private void sairDoSistema() {
        int confirmation = JOptionPane.showConfirmDialog(this, "Deseja sair do sistema?", "Confirmação",
                JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            // Implement here the code to exit the system
            // For example, you can use System.exit(0) to terminate the application
            System.exit(0);
        }
    }

    public void listaOrdenada() {
        // Show the option dialog with the buttons
        int option = JOptionPane.showOptionDialog(
                this,
                "Escolha o parâmetro de ordenação",
                "Ordenar Cadáveres",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new Object[] { "Nome", "CPF" },
                null);

        // Sort the cadaveres list based on the user's choice
        if (option == 0) {
            // Sort by name
            Collections.sort(cadaveres, Comparator.comparing(Cadaver::getNome));
        } else if (option == 1) {
            // Sort by CPF
            Collections.sort(cadaveres, Comparator.comparing(Cadaver::getCpf));
        } else {
            // If the user closes the dialog or doesn't make a selection, return
            return;
        }

        // Display the ordered list using the mostrarCadaver() method
        mostrarCadaverOrdenado();
    }

    public void buscarRegistros() {
        // Show the option dialog with the buttons
        int option = JOptionPane.showOptionDialog(
                this,
                "Escolha o parâmetro de busca",
                "Buscar Cadáveres",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new Object[] { "Nome", "CPF" },
                null);

        // Check the user's choice and perform the search
        if (option == 0) {
            buscarRegistrosPorNome();
        } else if (option == 1) {
            buscarRegistrosPorCPF();
        } else {
            // If the user closes the dialog or doesn't make a selection, return
            return;
        }
    }

    public void buscarRegistrosPorNome() {
        // Ask the user for the search query
        String searchQuery = JOptionPane.showInputDialog(this, "Digite o Nome para buscar:");

        if (searchQuery == null) {
            // User canceled the input or closed the dialog
            return;
        }

        // Create lists to store the search results
        List<Cadaver> searchResults = new ArrayList<>();

        // Perform the search based on the user's choice (name or CPF)
        for (Cadaver cadaver : cadaveres) {
            if (searchQuery != null) {
                if (cadaver.getNome().toLowerCase().contains(searchQuery.toLowerCase())) {
                    searchResults.add(cadaver);
                }
            }

        }

        // Check if any results were found
        if (searchResults.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum cadáver encontrado para a busca.");
            return;
        }

        // Display the search results using the mostrarCadaverOrdenado() method
        mostrarCadaverOrdenado(searchResults);
    }

    public void buscarRegistrosPorCPF() {

        JTextField cpfField = new JTextField(15);

        // Set placeholder for CPF field
        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cpfField.getText().equals("Apenas Números")) {
                    cpfField.setText("");
                    cpfField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (cpfField.getText().isEmpty()) {
                    cpfField.setText("Apenas Números");
                    cpfField.setForeground(Color.GRAY);
                }
            }
        });
        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Digite o CPF do Registro:"));
        panel.add(cpfField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Digite o CPF", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            // Ask the user for the search query
            String searchQuery = cpfField.getText().replaceAll("[^0-9]", "");

            // Create lists to store the search results
            List<Cadaver> searchResults = new ArrayList<>();

            // Perform the search based on the user's choice (name or CPF)
            for (Cadaver cadaver : cadaveres) {
                if (cadaver.getCpf().equals(formatCPF(searchQuery))) {
                    searchResults.add(cadaver);
                }
            }

            // Check if any results were found
            if (searchResults.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum cadáver encontrado para a busca.");
                return;
            }

            // Display the search results using the mostrarCadaverOrdenado() method
            mostrarCadaverOrdenado(searchResults);
        }

    }

    public void apagarRegistroPorCPF() {

        JTextField cpfField = new JTextField(15);

        // Set placeholder for CPF field
        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cpfField.getText().equals("Apenas Números")) {
                    cpfField.setText("");
                    cpfField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (cpfField.getText().isEmpty()) {
                    cpfField.setText("Apenas Números");
                    cpfField.setForeground(Color.GRAY);
                }
            }
        });
        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Digite o CPF para apagar o registro:"));
        panel.add(cpfField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Digite o CPF", JOptionPane.OK_CANCEL_OPTION);

        String searchQuery = cpfField.getText();

        if (searchQuery == null) {
            // User canceled the input or closed the dialog
            return;
        }

        if (result == JOptionPane.OK_OPTION) {
            searchQuery = searchQuery.replaceAll("[^0-9]", ""); // Remove non-numeric characters
            searchQuery = formatCPF(searchQuery);

            // Read all lines from the CSV file
            try {
                List<String> lines = Files.readAllLines(Paths.get(common.Paths.getDataPath()));
                boolean found = false;

                // Create a temporary list to store lines that don't match the searched CPF
                List<String> updatedLines = new ArrayList<>();
                String linha = "";

                // Search for the record with the given CPF and remove it from the lines list
                for (String line : lines) {
                    if (!line.startsWith(searchQuery + ";")) {
                        updatedLines.add(line);
                    } else {
                        found = true;
                        linha = line;
                    }
                }

                // If the CPF is not found, display a message and return
                if (!found) {
                    JOptionPane.showMessageDialog(this, "CPF não encontrado no arquivo.");
                    return;
                }

                Cadaver corpo = Cadaver.parseCadaver(linha);

                // Confirm the deletion before proceeding
                int confirmation = JOptionPane.showConfirmDialog(this,
                        "Deseja apagar o registro com o CPF: " + searchQuery + "?" +
                                "\nNome: " + corpo.getNome() + "\nSituação: " + corpo.getSituacao(),
                        "Confirmação", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_OPTION) {
                    if (autenticarAdmin()) {
                        // Write the updated data back to the CSV file
                        Files.write(Paths.get(common.Paths.getDataPath()), updatedLines, StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING);

                        JOptionPane.showMessageDialog(this, "Registro apagado com sucesso!");
                    }
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao acessar o arquivo: " + e.getMessage());
            }
        }

    }

    public void alterarSituacaoCadaver() {
        JTextField cpfField = new JTextField(15);

        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cpfField.getText().equals("Apenas Números")) {
                    cpfField.setText("");
                    cpfField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (cpfField.getText().isEmpty()) {
                    cpfField.setText("Apenas Números");
                    cpfField.setForeground(Color.GRAY);
                }
            }
        });
        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Digite o CPF para alterar a situação:"));
        panel.add(cpfField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Digite o CPF", JOptionPane.OK_CANCEL_OPTION);

        String searchQuery = cpfField.getText();

        if (searchQuery == null || searchQuery.equals("Apenas Números")) {
            return;
        }

        if (result == JOptionPane.OK_OPTION) {
            searchQuery = searchQuery.replaceAll("[^0-9]", "");
            searchQuery = formatCPF(searchQuery);

            // Read all lines from the file and update the situacao field
            try {
                List<String> lines = Files.readAllLines(Paths.get(common.Paths.getDataPath()));
                boolean found = false;

                // Update the situacao for the record with the given CPF
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith(searchQuery + ";")) {
                        String[] parts = line.split(";");
                        String nome = parts[1];
                        String novaSituacao = JOptionPane.showInputDialog(null, "Nova Situação para " + nome + ":");
                        if (novaSituacao != null && !novaSituacao.isEmpty()) {
                            parts[5] = novaSituacao;
                            lines.set(i, String.join(";", parts));
                            found = true;
                        } else {
                            JOptionPane.showMessageDialog(null, "A nova situação não pode ser vazia.");
                        }
                        break;
                    }
                }

                if (!found) {
                    JOptionPane.showMessageDialog(null, "CPF não encontrado no arquivo.");
                    return;
                }

                Files.write(Paths.get(common.Paths.getDataPath()), lines, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);

                JOptionPane.showMessageDialog(null, "Situação atualizada com sucesso!");

            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Erro ao acessar o arquivo: " + e.getMessage());
            }
        }
    }

    public void abrirOpcoesAdmin() {
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));

        JButton adicionarAdminButton = new JButton("Adicionar Admin");
        adicionarAdminButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        adicionarAdminButton.addActionListener(e -> AdicionarAdmin());

        JButton removerAdminButton = new JButton("Remover Admin");
        removerAdminButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        removerAdminButton.addActionListener(e -> removerAdmin2());

        JButton cancelButton = new JButton("Cancelar");
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(e -> EmDesenvolvimento());

        optionsPanel.add(Box.createVerticalStrut(20));
        optionsPanel.add(adicionarAdminButton);
        optionsPanel.add(Box.createVerticalStrut(20));
        optionsPanel.add(removerAdminButton);
        optionsPanel.add(Box.createVerticalStrut(20));
        optionsPanel.add(cancelButton);

        // Set the preferred size to make the box 2 times bigger
        optionsPanel.setPreferredSize(
                new Dimension(optionsPanel.getPreferredSize().width, optionsPanel.getPreferredSize().height * 2));

        JOptionPane.showOptionDialog(
                this,
                optionsPanel,
                "Opções de Administrador",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new Object[] {},
                null);
    }

    // Adicionando um funcionario na aba administração
    private void AdicionarAdmin() {
        JTextField cpfField = new JTextField(15);
        JTextField nomeField = new JTextField(15);
        JTextField login_acessoField = new JTextField(15);
        JPasswordField senhaField = new JPasswordField(15);
        JTextField cargoField = new JTextField(15);

        // Set placeholder for CPF field
        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cpfField.getText().equals("Apenas Números")) {
                    cpfField.setText("");
                    cpfField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (cpfField.getText().isEmpty()) {
                    cpfField.setText("Apenas Números");
                    cpfField.setForeground(Color.GRAY);
                }
            }
        });
        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        // Set placeholder for Hora de Falacimento field
        senhaField.setEchoChar('*'); // Define o caractere de eco como '*'

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("CPF:"));
        panel.add(cpfField);
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Login:"));
        panel.add(login_acessoField);
        panel.add(new JLabel("Criar senha:"));
        panel.add(senhaField);
        panel.add(new JLabel("Cargo:"));
        panel.add(cargoField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Adicionar Funcionário", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String cpf = cpfField.getText().replaceAll("[^0-9]", ""); // Remove non-numeric characters from the input
            String nome = nomeField.getText();
            String login_acesso = login_acessoField.getText();
            // String senha = senhaField.getText();
            char[] senhaChars = senhaField.getPassword();
            String senha = new String(senhaChars);
            String cargo = cargoField.getText();

            if (confirmarCampos(cpf, nome, login_acesso, senha, cargo)) {
                // Show a confirmation dialog before adding the record
                String message = "Deseja adicionar o seguinte Funcionário?\n\n"
                        + "CPF: " + formatCPF(cpf) + "\n"
                        + "Nome: " + nome + "\n"
                        + "Login: " + login_acesso + "\n"
                        + "Senha: " + senha + "\n"
                        + "Cargo: " + cargo + "\n";

                int confirmation = JOptionPane.showConfirmDialog(this, message, "Confirmação",
                        JOptionPane.YES_NO_OPTION);

                if (confirmation == JOptionPane.YES_OPTION) {
                    Funcionario funcionario = new Funcionario(formatCPF(cpf), nome, login_acesso, senha, cargo);
                    funcionarios.add(funcionario);
                    escreverFuncionarioNoArquivo();// COLOCAR CONFIRMAÇÃO QUE FOI ADICIONADO
                    JOptionPane.showMessageDialog(this, "Funcionário adicionado com sucesso!");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Preencha todos os campos antes de adicionar o funcionário.");
            }
        }
    }

    // esse está encontrando atraves do cpf

    public void apagarFuncionarioPorCPF() {
        JTextField cpfField = new JTextField(15);

        // Adicionar o FocusListener ao campo de CPF
        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Código a ser executado quando o campo de CPF ganhar foco
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Código a ser executado quando o campo de CPF perder foco
            }
        });

        // Configurar o placeholder e cor do texto
        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Digite o CPF para apagar o funcionário:"));
        panel.add(cpfField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Digite o CPF", JOptionPane.OK_CANCEL_OPTION);

        String searchQuery = cpfField.getText();

        if (searchQuery == null) {
            // Usuário cancelou a entrada ou fechou a caixa de diálogo
            return;
        }

        if (result == JOptionPane.OK_OPTION) {
            searchQuery = searchQuery.replaceAll("[^0-9]", ""); // Remove caracteres não numéricos
            searchQuery = formatCPF(searchQuery);

            // Verificar se o CPF está vazio
            if (searchQuery.isEmpty()) {
                JOptionPane.showMessageDialog(this, "CPF inválido.");
                return;
            }

            boolean found = false;
            Funcionario funcionarioRemovido = null;

            // Procurar pelo funcionário com o CPF fornecido e removê-lo da lista
            for (Funcionario funcionario : funcionarios) {
                if (funcionario.getCpf().equals(searchQuery)) {
                    funcionarioRemovido = funcionario;
                    found = true;
                    break; // Encontrou o funcionário, pode sair do loop
                }
            }

            // Se o CPF não foi encontrado, exibir uma mensagem e retornar
            if (!found) {
                JOptionPane.showMessageDialog(this, "Funcionário com o CPF fornecido não encontrado.");
                return;
            }

            // Confirmar a remoção antes de prosseguir
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Deseja apagar o funcionário com o CPF: " + searchQuery + "?" +
                            "\nNome: " + funcionarioRemovido.getNome() + "\nCargo: " + funcionarioRemovido.getCargo(),
                    "Confirmação", JOptionPane.YES_NO_OPTION);

            if (confirmation == JOptionPane.YES_OPTION) {
                if (autenticarAdmin()) {
                    // Remover o funcionário da lista
                    Iterator<Funcionario> iterator = funcionarios.iterator();
                    while (iterator.hasNext()) {
                        Funcionario funcionario = iterator.next();
                        if (funcionario.getCpf().equals(searchQuery)) {
                            funcionarioRemovido = funcionario;
                            iterator.remove(); // Remover o funcionário usando o Iterator
                            found = true;
                            break; // Encontrou o funcionário, pode sair do loop
                        }
                    }

                    // Atualizar o arquivo de dados (se necessário) - você precisa implementar essa
                    // parte

                    JOptionPane.showMessageDialog(this, "Funcionário apagado com sucesso!");
                }
            }
        }
    }

    public void removerAdmin2() {

        JTextField cpfField = new JTextField(15);

        // Set placeholder for CPF field
        cpfField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cpfField.getText().equals("Apenas Números")) {
                    cpfField.setText("");
                    cpfField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (cpfField.getText().isEmpty()) {
                    cpfField.setText("Apenas Números");
                    cpfField.setForeground(Color.GRAY);
                }
            }
        });
        cpfField.setText("Apenas Números");
        cpfField.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Digite o CPF para apagar o Funcionário:"));
        panel.add(cpfField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Digite o CPF", JOptionPane.OK_CANCEL_OPTION);

        String searchQuery = cpfField.getText();

        if (searchQuery == null) {
            // User canceled the input or closed the dialog
            return;
        }

        if (result == JOptionPane.OK_OPTION) {
            searchQuery = searchQuery.replaceAll("[^0-9]", ""); // Remove non-numeric characters
            searchQuery = formatCPF(searchQuery);

            // Read all lines from the CSV file
            try {
                List<String> lines = Files.readAllLines(Paths.get(common.Paths.getDataPath()));
                boolean found = false;

                // Create a temporary list to store lines that don't match the searched CPF
                List<String> updatedLines = new ArrayList<>();
                String linha = "";

                // Search for the record with the given CPF and remove it from the lines list
                for (String line : lines) {
                    if (!line.startsWith(searchQuery + ";")) {
                        updatedLines.add(line);
                    } else {
                        found = true;
                        linha = line;
                    }
                }

                // If the CPF is not found, display a message and return
                if (!found) {
                    JOptionPane.showMessageDialog(this, "CPF não encontrado no arquivo.");
                    return;
                }

                Funcionario funcionario = Funcionario.parseFuncionario(linha);

                // Confirm the deletion before proceeding
                int confirmation = JOptionPane.showConfirmDialog(this,
                        "Deseja apagar o Funcionario com o CPF: " + searchQuery + "?" +
                                "\nNome: " + funcionario.getNome() + "\nCargo: " + funcionario.getCargo(),
                        "Confirmação", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_OPTION) {
                    if (autenticarAdmin()) {
                        // Write the updated data back to the CSV file
                        Files.write(Paths.get(common.Paths.getDataPath()), updatedLines, StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING);

                        JOptionPane.showMessageDialog(this, "Registro apagado com sucesso!");
                    }
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao acessar o arquivo: " + e.getMessage());
            }
        }

    }

    public void Cancelar() {
        int confirmation = JOptionPane.showConfirmDialog(optionsPanel, "Deseja encerrar a área de administração?",
                "Confirmação",
                JOptionPane.YES_NO_OPTION);
    }

    public static void main(String[] args) {
        Connection conn = DB.getConnection();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Controlador(conn);
            }
        });
    }
}
