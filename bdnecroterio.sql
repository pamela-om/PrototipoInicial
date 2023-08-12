CREATE TABLE cadaver (
	identificacao CHAR(14) PRIMARY KEY,
    nome_cadaver VARCHAR(100) NOT NULL,
    peso DOUBLE,
    dataMorte VARCHAR(10),
    horaMorte VARCHAR(5),
    situacao VARCHAR(50) NOT NULL
    );
