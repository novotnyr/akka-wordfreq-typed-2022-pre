package sk.upjs.ics.kopr;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Routers;

import java.util.HashMap;
import java.util.Map;

public class Coordinator extends AbstractBehavior<Coordinator.Command> {
    private ActorRef<WordFrequencyCounter.Command> worker;

    private ActorRef<WordFrequencyCounter.FrequenciesCalculated> messageAdapter;

    private Map<String, Long> globalFrequencies = new HashMap<>();

    private Coordinator(ActorContext<Command> context) {
        super(context);
        this.worker = context.spawn(createPool(), "worker");
        this.messageAdapter = context.messageAdapter(WordFrequencyCounter.FrequenciesCalculated.class,
                this::adaptFrequenciesCalculated
                );
        context.watch(worker);
    }

    private static Behavior<WordFrequencyCounter.Command> createPool() {
        return Routers.pool(3, createSupervisedWorker());
    }

    private static Behavior<WordFrequencyCounter.Command> createSupervisedWorker() {
        /*
        return Behaviors.supervise(
                    Behaviors.supervise(WordFrequencyCounter.create())
                         .onFailure(IllegalStateException.class, SupervisorStrategy.restart()))
                .onFailure(UnsupportedOperationException.class, SupervisorStrategy.resume());
            */
        return Behaviors.supervise(WordFrequencyCounter.create())
                 .onFailure(IllegalStateException.class, SupervisorStrategy.restart());
    }

    private Command adaptFrequenciesCalculated(WordFrequencyCounter.FrequenciesCalculated event) {
        Map<String, Long> frequencies = event.frequencies();
        return new AggregateFrequencies(frequencies);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Coordinator::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CalculateFrequencies.class, this::calculateFrequencies)
                .onMessage(AggregateFrequencies.class, this::aggregateFrequencies)
                .onSignal(Terminated.class, this::terminated)
                .build();
    }

    private Behavior<Command> terminated(Terminated signal) {
        getContext().getLog().error("Terminating worker pool!");

        return Behaviors.stopped();
    }

    private Behavior<Command> aggregateFrequencies(AggregateFrequencies command) {
        Map<String, Long> frequencies = command.frequencies();
        frequencies.forEach((word, frequency) -> {
            this.globalFrequencies.merge(word, frequency, Long::sum);
        });

        getContext().getLog().debug("Global frequencies: {}", this.globalFrequencies);
        return Behaviors.same();
    }

    private Behavior<Command> calculateFrequencies(CalculateFrequencies command) {
        var sentence = command.sentence();
        this.worker.tell(new WordFrequencyCounter.CalculateFrequencies(sentence, messageAdapter));
        return Behaviors.same();
    }

    public interface Command {}

    public interface Event{}

    public record CalculateFrequencies(String sentence) implements Command{}

    public record AggregateFrequencies(Map<String, Long> frequencies) implements Command {}
}
