package com.myorg;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;

public class SnsStack extends Stack {

//    Esse atributo representa o tópico que será criado. Com isso quando formos precisar desse tópico
//    no service, agente vai pegar ele daqui.
    private final SnsTopic postEventsTopic;

    public SnsStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SnsStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        postEventsTopic = SnsTopic
                .Builder
                .create(buildPostEventsTopic())
                .build();

//        Inscreve nosso e-mail no tópico para receber os eventos.
        postEventsTopic.getTopic().addSubscription(buildEmailSubscription());

    }



    public SnsTopic getPostEventsTopic() {
        return postEventsTopic;
    }

    @NotNull
    private Topic buildPostEventsTopic() {
        return Topic.Builder.create(this,"PostEventsTopic")
                .topicName("post-events")
                .build();
    }


    @NotNull
    private EmailSubscription buildEmailSubscription() {
//        EmailSubscription é o tipo de inscrição que vou fazer no tópico
        return EmailSubscription.Builder.create("geovane.freitasc@gmail.com").json(true).build();
    }
}