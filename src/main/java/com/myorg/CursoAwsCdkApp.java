package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Arrays;

public class CursoAwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

//Todas as minhas stacks são definidas aqui

//Primeiro parâmetro é o escopo e nele passamos o aplicativo que estamos criando e onde vamos criar
//nosso recurso. O segundo parâmetro é um id
        VpcStack vpcStack = new VpcStack(app, "Vpc");

//Dentro da stack do meu cluster estou passando a vpc a qual ele pertence
        ClusterStack clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
//        Para criar um cluster eu preciso primeiro ter criado uma vpc, então informo que o cluster
//        depende da criação do vpc
        clusterStack.addDependency(vpcStack);

//Primeiro parâmetro é o nosso escopo, o 2º é Rds que é a identificação,
        RdsStack rdsStack = new RdsStack(app, "Rds", vpcStack.vpc);
//        Essa stack depende da stack de vpc
        rdsStack.addDependency(vpcStack);

//        Coloco a instância do tópico aqui porque ele será usado no nosso service, então precisa
//        ser criado antes dele
        SnsStack snsStack = new SnsStack(app, "Sns");

        Service01Stack service01Stack = new Service01Stack(app, "Service01",
                clusterStack.getCluster(), snsStack.getPostEventsTopic());
//        Para criar o Service01Stack eu preciso primeiro ter criado um cluster, então eu dependo dele.
        service01Stack.addDependency(clusterStack);
//        A nossa stack que faz o deployment do nosso serviço, também depende da stack que cria o BD
        service01Stack.addDependency(rdsStack);
//        A nossa stack que faz o deployment do nosso serviço, também depende da stack do sns

        service01Stack.addDependency(snsStack);

        Service02Stack service02Stack = new Service02Stack(app, "Service02",
                clusterStack.getCluster());

        app.synth();
    }
}
