package com.myorg;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;

import java.util.HashMap;
import java.util.Map;

public class Service01Stack extends Stack {
    //    Com o parÂmetro Cluster estou especificando qual custer vamos utilizar
    public Service01Stack(final Construct scope, final String id, Cluster cluster, SnsTopic postEventsTopic) {
        this(scope, id, null, cluster, postEventsTopic);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster, SnsTopic postEventsTopic) {
        super(scope, id, props);

//        Vamos utilizar o valor que estão sendo exportados no RDSStack e vamos passar para a nossa aplicaçãp
//        por meio de variáveis de ambiente.
        Map<String, String> environmentVariables = new HashMap<>();
//        O valor do endpoint do BD está sendo do valor que está sendo exportado na stack do RDS
//        jdbc:mariadb: é o conector que nós vamos utilizar + o endpoint de acesso que é o endereço da nossa instância
//        + a porta configurada na stack do RDS + o nome do schema que irá conter a nossa tabela (o schema )
//        é criado por nós.

//        O spring boot converte os nomes para minusculo e onde está '_' ele converte para '.'
//        então por isso a aplicação quando está na AWS consegue acessar as variáveis de ambiente do
//        container e passar os valores dela para a aplicação.
        environmentVariables.put("SPRING_DATASOURCE_URL", "jdbc:mariadb://"+Fn.importValue("rds-endpoint")+ ":3306/aws_project01?createDatabaseIfNotExist=true");

// Usuário utilizado na definição da instância do banco de dados
        environmentVariables.put("SPRING_DATASOURCE_USERNAME", "admin");
//        Importo a variável exportada pelo RdsStack que é a senha utilizada para configurar a instância

        environmentVariables.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("rds-password"));

        environmentVariables.put("AWS_REGION", "us-east-1");
//        Pego o tópico que foi criado e acesso o Arn (Amazon Resource Name)
        environmentVariables.put("AWS_SNS_TOPIC_POST_EVENTS_ARN", postEventsTopic.getTopic().getTopicArn());

        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService
                .Builder
                .create(this, "ALB01")
//                Nome que vai aparecer dentro do cluster
                .serviceName("service-01")
//                Qual o cluster que eu vou querer usar esse serviço
                .cluster(cluster)
//                Quanto de CPU nós vamos utilizar
                .cpu(512)
//                Quantidade de memória que vamos utilizar na nossa aplicação
                .memoryLimitMiB(1024)
//                Quantidade de instâncias desejadas assim que a aplicação subir
                .desiredCount(2)
//                porta que vai ser liberada para acesso externo
                .listenerPort(8080)
//                Criação da nossa task, tarefa, para especificar como que o nosso serviço
//                vai ser executado. É uma receita de como executar a aplicação, em termos de recursos
//                computacionais e principalmente a versão da imagem docker.
                .taskImageOptions(
//                        Imagem do docker que vamos utilizar dentro do nosso serviço
                        buildApplicationLoadBalancedTaskImageOptions(environmentVariables)
                )
//                Digo que meu load balancer é público. Eu poderia criar um interno também, que somente
//                os meus recursos da aws teriam acesso a ele.

//                Vamos ter um DNS que vai poder acessar esse aplication load balancer e de fato
//                acessar no aplicação
                .publicLoadBalancer(true)
                .build();

//        Acesso as configurações do HealthCheck para saber a saúde da aplicação, monitora a saúde das instâncias
        buildHealthCheckConfig(service01);

//        Auto scaling é um recurso que monitora parâmetros como CPU, RAM, quantidade de requisições
//        e cria novas instâncias da aplicação se precisar de mais recursos, para que não pare de
//        atender as requisições que estão chegando.
        buildAutoScaling(service01);

//        Adiciono uma permissão no tópico de publicar (grantPublish)
//        Quero dar essa permissão para a definição da tarefa do meu servico

        postEventsTopic.getTopic().grantPublish(service01.getTaskDefinition().getTaskRole());
    }



    private void buildHealthCheckConfig(ApplicationLoadBalancedFargateService service01) {
        service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
//                Qual que é o caminho que o application loadbalancer deve ficar monitorando pra
//                saber se a nossa app está rodando ou não.
                        .path("/actuator/health")
                        .port("8080")
//                Qual o código de resposta esperado pra saber se a nossa instância está viva ou não.
                        .healthyHttpCodes("200")
                        .build()
        );
    }

    private ApplicationLoadBalancedTaskImageOptions buildApplicationLoadBalancedTaskImageOptions(Map<String, String> environmentVariables) {
        return ApplicationLoadBalancedTaskImageOptions.builder()
//                                Nome do container
                .containerName("aws_project01")
//                        Localização da nossa imagem, pegar do docker hub e adicionar a versão da tag que vou usar
                .image(ContainerImage.fromRegistry("fcgeovane/instadev_aws01:1.8.0"))
//                      Porta que a minha aplicação vai rodar
                .containerPort(8080)
//                      Onde que os logs da minha aplicação vão aparecer
                .logDriver(
//                      Defino que os logs vão ser redirecionados para o Cloud Watch
//                      onde vamos poder observar o que está acontecendo no container.
//                      com isso não vamos precisar entrar dentro da máquina que está executando
//                      a nossa aplicação para poder ver nossos logs
                        buildLogDriver()
                )
//                Vamos inserir variáveis de ambiente dentro da definição da nossa tarefa criada
//                no ECS
                .environment(environmentVariables)
                .build();
    }

    private LogDriver buildLogDriver() {
        return LogDriver.awsLogs(
                AwsLogDriverProps.builder()
                        .logGroup(
                                buildLogGroup())
//                                                        Pequenos arquivos que são reciclados de tempos em tempos
//                                                        onde nossos logs vão está
//                                                        Service01 é um prefixo desse streams para poder localizar todos eles
                        .streamPrefix("Service01")
                        .build());
    }

    private LogGroup buildLogGroup() {
        return LogGroup.Builder.create(this, "Service01Loggroup")
//                                                                Nome que vai agrupar todos os meus logs
                .logGroupName("Service01")
//                                                                Política de remoção de logs
//                                                                quando eu precisar apagar tudo isso daqui, ele vai lá
//                                                                e apaga todos os logs
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private void buildAutoScaling(ApplicationLoadBalancedFargateService service01) {
        //        Define qual é a capacidade mínima e máxima do meu auto scaling
        ScalableTaskCount scalableTaskCount =
//              Pego o aplication load balancer que eu criei
                service01.
//                        pego o serviço que foi criado dentro dele
        getService().
//                        Pego as configs de task count
        autoScaleTaskCount(EnableScalingProps.builder()
//                Quero que tenha pelo menos 2 instâncias da minha app executando.
                .minCapacity(2)
//                Quero que tenha no máximo 4 instâncias da minha app executando.

                .maxCapacity(4)
                .build()
);

//        Defino quais são os parâmetros que esse auto scaling vai atuar
        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
//                Se o consumo médio de CPU ultrapassar 50% , em um intervalo de 60 segundos, então
//                cria-se uma nova instância obdecendo a capacidade máxima de 4 instâncias.
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
//                Período de análise para poder distruir as instãncias. Se eu tiver durante 60 segundos
//                um consumo médio a baixo de 50% ele vai lá e distrói uma instância obedecendo o mínimo
//                de duas instâncias
                .scaleOutCooldown(Duration.seconds(60))
                .build());
    }
}
