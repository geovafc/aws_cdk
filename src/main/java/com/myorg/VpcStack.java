package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;

public class VpcStack extends Stack {
    Vpc vpc;

    public VpcStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public VpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        //Criação da VPC
        //Escopo, que é a classe que estamos e o segundo parâmetro é um identificador
        vpc = Vpc.Builder.create(this, "Vpc01")
                //Quantidade máxima de zonas de disponibilidade que a nossa VPC vai atuar
                .maxAzs(3)
                .build();
    }

    public Vpc getVpc() {
        return vpc;
    }
}
