package com.myorg;

import software.amazon.awscdk.core.CfnParameter;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.*;

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
    }
}
