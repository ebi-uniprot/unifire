package uk.ac.ebi.uniprot.urml.engine.drools.fixtures;

import lombok.experimental.UtilityClass;
import org.uniprot.urml.rules.*;

import javax.xml.namespace.QName;

/**
 * Test-only fluent DSL for building URML rules without the noisy
 * {@link DisjunctiveConditionSet}/{@link ConjunctiveConditionSet} boilerplate.
 *
 * <p>The DSL delegates to the production builders so it cannot drift from real URML
 * semantics. It is intentionally limited to the shapes needed by the converter tests.</p>
 */
@UtilityClass
public final class UrmlRuleDsl {

    public static RuleBuilder rule(String id) {
        return new RuleBuilder(id);
    }

    public static final class RuleBuilder {
        private final Rule.Builder<Void> delegate;
        private final ActionsBuilder actionsBuilder = new ActionsBuilder();
        private DisjunctiveConditionSet.Builder conditions;

        private RuleBuilder(String id) {
            this.delegate = Rule.builder().withId(id);
        }

        public RuleBuilder status(RuleStatus status) {
            delegate.withStatus(status);
            return this;
        }

        public RuleBuilder procedural() {
            delegate.withProcedural(true);
            return this;
        }

        public RuleBuilder extendsRule(Rule parent) {
            delegate.withExtends(parent);
            return this;
        }

        public RuleBuilder when(ConditionBuilder first, ConditionBuilder... rest) {
            return withConditions(first, rest);
        }

        public RuleBuilder withConditions(ConditionBuilder first, ConditionBuilder... rest) {
            Condition[] all = new Condition[rest.length + 1];
            all[0] = first.build();
            for (int i = 0; i < rest.length; i++) {
                all[i + 1] = rest[i].build();
            }
            return withConditions(all);
        }

        public RuleBuilder withConditions(DisjunctiveConditionSet conditions) {
            this.conditions = DisjunctiveConditionSet.builder();
            this.conditions.addAND(conditions.getAND().toArray(new ConjunctiveConditionSet[0]));
            return this;
        }

        private RuleBuilder withConditions(Condition... conditions) {
            DisjunctiveConditionSet.Builder builder = DisjunctiveConditionSet.builder();
            builder.addAND(ConjunctiveConditionSet.builder().addCondition(conditions).build());
            this.conditions = builder;
            return this;
        }

        public RuleBuilder then(Action... actions) {
            return withActions(actions);
        }

        public RuleBuilder withActions(Action... actions) {
            actionsBuilder.add(actions);
            return this;
        }

        public Rule build() {
            if (conditions != null) {
                delegate.withConditions(conditions.build());
            }
            return delegate.withActions(actionsBuilder.build()).build();
        }

        public Rules buildRules() {
            return Rules.builder()
                    .withName("org.uniprot.unirule.test")
                    .withVersion("1.0")
                    .addRule(build())
                    .build();
        }
    }

    public static ConditionBuilder condition(QName factType) {
        return new ConditionBuilder(Condition.builder().withOn(factType));
    }

    public static final class ConditionBuilder {
        private final Condition.Builder<?> delegate;

        private ConditionBuilder(Condition.Builder<?> delegate) {
            this.delegate = delegate;
        }

        public ConditionBuilder as(String bind) {
            delegate.withBind(bind);
            return this;
        }

        public ConditionBuilder bind(String bind) {
            return as(bind);
        }

        public ConditionBuilder filter(Filter filter) {
            delegate.withFilter(filter);
            return this;
        }

        public ConditionBuilder of(String of) {
            delegate.withOf(of);
            return this;
        }

        public ConditionBuilder with(String with) {
            delegate.withWith(with);
            return this;
        }

        public ConditionBuilder exists(boolean exists) {
            delegate.withExists(exists);
            return this;
        }

        public ConditionBuilder collect(boolean collect) {
            delegate.withCollect(collect);
            return this;
        }

        public Condition build() {
            return delegate.build();
        }
    }

    private static final class ActionsBuilder {
        private final org.uniprot.urml.rules.Actions.Builder delegate = org.uniprot.urml.rules.Actions.builder();

        void add(Action... actions) {
            delegate.addAction(actions);
        }

        org.uniprot.urml.rules.Actions build() {
            return delegate.build();
        }
    }
}
