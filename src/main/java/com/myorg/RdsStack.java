package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;

import java.util.Collections;

public class RdsStack extends Stack {
    public RdsStack(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
        super(scope, id, props);

//        Precisamos definir qual que é a máquina que vai rodar a nossa instância, qual tipo de máquina
//        em termo de CPU e memória

//        Precisamos definir qual que é a estratégia para poder acessar essa instância (usuário e senha
//        que serão passados como parâmetros).

//        Essa instância não estará liberada para acesso público, somente para os recursos que estão
//        dentro da VPC.

//        Cfn = Cloud Formation Parameter
//        Parameter que será usada no deploy com as informações da senha do banco de dados. databasePassword
//        é o nome do parâmetro
        CfnParameter databasePassword = CfnParameter.Builder.create(this, "databasePassword")
                .type("String")
                .description("The RDS instance password")
                .build();

//        Abrir a porta de acesso para a instância RDS

//        Nós temos o nosso security group que precisa ter as configurações de quais portas ele acessa

//        Aqui estou buscando qual é o security group que nós temos, a partir da nossa VPC
        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
//       Adiciona uma nova regra de entrada nesse security group. Qualquer IP da internet pode ter acesso através
//        do protocolo TCP na porta 3306, porém essa configuração permite que somente quem estiver dentro
//        da VPC possa acessar essa porta.
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));


//        Criação da minha instância RDS
        DatabaseInstance databaseInstance = DatabaseInstance.Builder
                .create(this, "Rds01")
//                Identificador da instância que será mostrado no console da AWS
                .instanceIdentifier("aws-project01-db")
//                Define o tipo da instância.
                .engine(DatabaseInstanceEngine.mysql(
//               Define as propriedades do tipo da instância
                        MySqlInstanceEngineProps.builder()
                                .version(MysqlEngineVersion.VER_5_7)
                        .build()))
//                Coloca a instância dentro da VPC que já foi criada e onde está o nosso cluster
                .vpc(vpc)
//                Define a estratégia de criação do mecanismo de credencial que agente tem pra acessar a
//                nossa instância. Estou dizendo que quero trabalhar com usuário e senha.
                .credentials(Credentials.fromUsername("admin",
                        CredentialsFromUsernameOptions.builder()
//               Pego o parâmetro de entrada que vamos utilizar na hora de criar a stack, a senha não
//               está fixa no meu código.
                                .password(SecretValue.plainText(databasePassword.getValueAsString()))
                                .build()))
//                Defino o tamanho da máquina que vai executar a minha instância.
//                Estou usando uma máquina t2Micro que é uma instância bem pequena que suporta
//                tranquilamente o nosso banco de dados, essa instância permite 40 conexões simultânea.
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
//                Não quero que a minha instância fique em várias zonas de disponibilidade
                .multiAz(false)
//                Tamanho do disco é de 10 gigas
                .allocatedStorage(10)
//                Coloco o security group definido acima
                .securityGroups(Collections.singletonList(iSecurityGroup))
//                Defino quais que são as sub-redes que vou colocar a minha instância
                .vpcSubnets(SubnetSelection.builder()
//                vou usar as mesmas sub-redes que tenho na minha vpc
                        .subnets(vpc.getPrivateSubnets())
                        .build())
                .build();

    }
}
