package com.myorg;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;

import java.util.HashMap;
import java.util.Map;

public class Service02Stack extends Stack {

//    Preciso configurar permissão para que este Service tenho acesso a tabela do DynamoDB.
//    para isso ser possível eu preciso dizer que essa stack recebe uma tabela como parâmetro: Table postEventsDynamoDb.


    public Service02Stack(final Construct scope, final String id, Cluster cluster, SnsTopic postEventsTopic, Table postEventsDynamoDb) {
        this(scope, id, null, cluster, postEventsTopic, postEventsDynamoDb);
    }

    public Service02Stack(final Construct scope, final String id, final StackProps props, Cluster cluster, SnsTopic postEventsTopic, Table postEventsDynamoDb) {
        super(scope, id, props);

//        Fila responsável por receber as mensagens que não forem tratadas pelo nosso consumidor (deram exceções na hora de tratar)
//        Quando formos utilizar o DLQ, vamos precisar dessa fila.
        DeadLetterQueue deadLetterQueue = buildDeadLetterQueue();

//        Fila principal responsável por receber as mensagens
        Queue postEventsQueue = Queue.Builder.create(this, "PostEvents")
                .queueName("post-events")
//                Fila para onde vou redirecionar as mensagens caso dê errado o tratamento no consumer
                .deadLetterQueue(deadLetterQueue)
                .build();

//        Inscreve a fila dentro do tópico do SNS
        SqsSubscription sqsSubscription = SqsSubscription.Builder
//                Crio uma inscrição para essa fila
                .create(postEventsQueue).
                build();
//      Pego o tópico e inscrevo a fila nele
        postEventsTopic.getTopic().addSubscription(sqsSubscription);



        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("AWS_REGION", "us-east-1");
//        Nome da fila criada
        environmentVariables.put("AWS_SQS_QUEUE_POST_EVENTS_NAME", postEventsQueue.getQueueName());

        ApplicationLoadBalancedFargateService service02 = ApplicationLoadBalancedFargateService
                .Builder
                .create(this, "ALB02")
//                Nome que vai aparecer dentro do cluster
                .serviceName("service-02")
//                Qual o cluster que eu vou querer usar esse serviço
                .cluster(cluster)
//                Quanto de CPU nós vamos utilizar
                .cpu(512)
//                Quantidade de memória que vamos utilizar na nossa aplicação
                .memoryLimitMiB(1024)
//                Quantidade de instâncias desejadas assim que a aplicação subir
                .desiredCount(2)
//                porta que vai ser liberada para acesso externo
                .listenerPort(9090)
//                Criação da nossa task, tarefa, para especificar como que o nosso serviço
//                vai ser executado. É uma receita de como executar a aplicação, em termos de recursos
//                computacionais e principalmente a versão da imagem docker.
                .taskImageOptions(
//                        Imagem do docker que vamos utilizar dentro do nosso serviço
                        buildApplicationLoadBalancedTaskImageOptions()
                )
//                Digo que meu load balancer é público. Eu poderia criar um interno também, que somente
//                os meus recursos da aws teriam acesso a ele.

//                Vamos ter um DNS que vai poder acessar esse aplication load balancer e de fato
//                acessar no aplicação
                .publicLoadBalancer(true)
                .build();

//        Acesso as configurações do HealthCheck para saber a saúde da aplicação, monitora a saúde das instâncias
        buildHealthCheckConfig(service02);

//        Auto scaling é um recurso que monitora parâmetros como CPU, RAM, quantidade de requisições
//        e cria novas instâncias da aplicação se precisar de mais recursos, para que não pare de
//        atender as requisições que estão chegando.
        buildAutoScaling(service02);

//        Atribuo a permissão para que esse serviço possa acessar e consumir as mensagens da fila.
//        Garanto a permissão do papel do service02 para que ele possa consumir mensagens da fila.
        postEventsQueue.grantConsumeMessages(service02.getTaskDefinition().getTaskRole());

//        Atribuo a permissão de acesso a tabela do DynamoDB.
//        Garanta a permissão de leitura e escrita para o papel que executa as tarefas deste Service
        postEventsDynamoDb.grantReadWriteData(service02.getTaskDefinition().getTaskRole());
    }

    @NotNull
    private DeadLetterQueue buildDeadLetterQueue() {
        Queue postEventsDlq = Queue.Builder.create(this, "PostEventsDlq")
                .queueName("post-events-dlq")
                .build();
//        Criação da DLQ que será utilizado depois na construção da fila principal.
        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
//                Fila que representa a minha dlq
                .queue(postEventsDlq)
//                Quantidade máxima de tentativas para tratar a mensagem no consumidor, antes de ser enviada para DLQ
                .maxReceiveCount(3)
                .build();
        return deadLetterQueue;
    }

    private ApplicationLoadBalancedTaskImageOptions buildApplicationLoadBalancedTaskImageOptions() {
        return ApplicationLoadBalancedTaskImageOptions.builder()
//                                Nome do container
                .containerName("aws_instadev02_consumer")
//                        Localização da nossa imagem, pegar do docker hub e adicionar a versão da tag que vou usar
                .image(ContainerImage.fromRegistry("fcgeovane/aws_instadev02_consumer:1.3.1"))
//                      Porta que a minha aplicação vai rodar
                .containerPort(9090)
//                      Onde que os logs da minha aplicação vão aparecer
                .logDriver(
//                      Defino que os logs vão ser redirecionados para o Cloud Watch
//                      onde vamos poder observar o que está acontecendo no container.
//                      com isso não vamos precisar entrar dentro da máquina que está executando
//                      a nossa aplicação para poder ver nossos logs
                        buildLogDriver()
                )
                .build();
    }

    private LogDriver buildLogDriver() {
        return LogDriver.awsLogs(
                AwsLogDriverProps.builder()
                        .logGroup(
                                buildLogGroup())
//                                                        Pequenos arquivos que são reciclados de tempos em tempos
//                                                        onde nossos logs vão está
//                                                        Service02 é um prefixo desse streams para poder localizar todos eles
                        .streamPrefix("Service02")
                        .build());
    }

    private LogGroup buildLogGroup() {
        return LogGroup.Builder.create(this, "Service02Loggroup")
//                                                                Nome que vai agrupar todos os meus logs
                .logGroupName("Service02")
//                                                                Política de remoção de logs
//                                                                quando eu precisar apagar tudo isso daqui, ele vai lá
//                                                                e apaga todos os logs
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private void buildHealthCheckConfig(ApplicationLoadBalancedFargateService service02) {
        service02.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
//                Qual que é o caminho que o application loadbalancer deve ficar monitorando pra
//                saber se a nossa app está rodando ou não.
                        .path("/actuator/health")
                        .port("9090")
//                Qual o código de resposta esperado pra saber se a nossa instância está viva ou não.
                        .healthyHttpCodes("200")
                        .build()
        );
    }

    private void buildAutoScaling(ApplicationLoadBalancedFargateService service02) {
        //        Define qual é a capacidade mínima e máxima do meu auto scaling
        ScalableTaskCount scalableTaskCount =
//              Pego o aplication load balancer que eu criei
                service02.
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
        scalableTaskCount.scaleOnCpuUtilization("Service02AutoScaling", CpuUtilizationScalingProps.builder()
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
