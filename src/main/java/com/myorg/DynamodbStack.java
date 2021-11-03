package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;

public class DynamodbStack extends Stack {

    private final Table postEventsDynamoDb;

    public DynamodbStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamodbStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        postEventsDynamoDb = Table.Builder.create(this,"PostEventsDynamoDb")
                .tableName("post-events")
                //limita a quantidade de leitura e escrita para evitar surpresas de cobrança
                .billingMode(BillingMode.PROVISIONED)
//                Capacidade de leitura limitada a uma unidade (tamanho específico que vc poder por segundo)
//                na sua tabela
                .readCapacity(1)
//                A intenção de definir o mínimo é para fazermos uma análise depois no teste de carga
//                e ver o que vai acontecer se agente esbarrar naquele mínimo.
                .writeCapacity(1)
// Atributos de cada item da nossa tabela

//                Estratégia de definição de chave primária usada será a de chave composta
//                A primeira chave é a pk ( partition key, poderia ser qualquer nome) que é do tipo String
//                O próximo atributo da nossa chave composta é o sort key.

//                Não é necessário que os demais campos sejam criados aqui, visto que cada item
//                pode ter a sua estrutura, desde que respeite essa estrutura base aqui.
                .partitionKey(
                        Attribute.builder()
                                .name("pk")
                                .type(AttributeType.STRING)
                                .build()
                )
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
//                Campo que define o tempo de vida do item dentro da tabela (nem todas tabelas precisam)
                .timeToLiveAttribute("ttl")
//                Estratégia em relação ao comportamento da nossa tabela frente a stack: se eu apagar a stack
//                o que eu quero que aconteça com a tabela. Se a stack que estamos criando aqui for apagada
//                este recurso aqui também será destruído. O ideal é que quando a stack for destruída
//                a tabela não seja, por que os dados precisam ficar retidos.
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    public Table getPostEventsDynamoDb() {
        return postEventsDynamoDb;
    }
}
