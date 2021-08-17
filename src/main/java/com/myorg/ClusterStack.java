package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;

//No momento em que formos criar o cluster, precisamos especificar em qual VPC ele será criado.
//faço isso passando como parâmetro no construtor o vpc
public class ClusterStack extends Stack {
    private Cluster cluster;

    public ClusterStack(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public ClusterStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
        super(scope, id, props);

        //Criação do Cluster
        //Escopo, que é a classe que estamos e o segundo parâmetro é um identificador
        cluster = Cluster.Builder.create(this, id)
                .clusterName("cluster-01")
//                Qual a vpc que esse cluster vai está inserido
                .vpc(vpc)
                .build();
    }

    public Cluster getCluster() {
        return cluster;
    }
}
