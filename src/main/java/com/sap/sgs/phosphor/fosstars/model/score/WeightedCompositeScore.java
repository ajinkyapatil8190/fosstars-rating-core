package com.sap.sgs.phosphor.fosstars.model.score;

import static com.sap.sgs.phosphor.fosstars.model.other.Utils.setOf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.sgs.phosphor.fosstars.model.Confidence;
import com.sap.sgs.phosphor.fosstars.model.Feature;
import com.sap.sgs.phosphor.fosstars.model.Score;
import com.sap.sgs.phosphor.fosstars.model.Tunable;
import com.sap.sgs.phosphor.fosstars.model.Value;
import com.sap.sgs.phosphor.fosstars.model.Visitor;
import com.sap.sgs.phosphor.fosstars.model.Weight;
import com.sap.sgs.phosphor.fosstars.model.value.ScoreValue;
import com.sap.sgs.phosphor.fosstars.model.value.ValueHashSet;
import com.sap.sgs.phosphor.fosstars.model.weight.ScoreWeights;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A base class for scores which are based on a weighted average of other scores (sub-scores).
 */
public class WeightedCompositeScore extends AbstractScore implements Tunable {

  /**
   * A set of weighted sub-scores.
   */
  private final Set<Score> subScores;

  /**
   * Weights of the sub-scores.
   */
  private final ScoreWeights weights;

  /**
   * Makes a score from a list of sub-scores.
   */
  public WeightedCompositeScore(String name, Score... subScores) {
    this(name, setOf(subScores), ScoreWeights.createFor(subScores));
  }

  /**
   * Initializes a new score.
   * This constructor is used by Jackson during deserialization.
   */
  @JsonCreator
  WeightedCompositeScore(
      @JsonProperty("name") String name,
      @JsonProperty("subScores") Set<Score> subScores,
      @JsonProperty("weights") ScoreWeights weights) {

    super(name);

    Objects.requireNonNull(subScores, "Sub-scores can't be null!");
    Objects.requireNonNull(weights, "Weights can't be null");

    if (subScores.isEmpty()) {
      throw new IllegalArgumentException("Sub-scores can't be empty!");
    }

    if (subScores.size() != weights.size()) {
      throw new IllegalArgumentException(
          "Hey! Number of sub-scores should be equal to number of weights!");
    }

    for (Score score : subScores) {
      if (!weights.of(score).isPresent()) {
        throw new IllegalArgumentException(
            String.format("Hey! You have to give me a weight for %s!", score.getClass()));
      }
    }

    this.subScores = subScores;
    this.weights = weights;
  }

  @Override
  @JsonProperty("subScores")
  public Set<Score> subScores() {
    return new HashSet<>(subScores);
  }

  @JsonGetter("weights")
  ScoreWeights weights() {
    return weights;
  }

  @Override
  public List<Weight> parameters() {
    return weights.parameters();
  }

  @Override
  @JsonIgnore
  public boolean isImmutable() {
    return weights.isImmutable();
  }

  @Override
  public void makeImmutable() {
    weights.makeImmutable();
  }

  /**
   * Calculate an overall score value as a weighted average of the underlying sub-scores.
   *
   * @param values A number of values.
   * @return An overall score.
   */
  @Override
  public final ScoreValue calculate(Value... values) {
    ValueHashSet valueSet = new ValueHashSet(values);

    double weightSum = 0.0;
    double scoreSum = 0.0;
    double confidenceSum = 0.0;

    ScoreValue scoreValue = new ScoreValue(this);
    boolean allNotApplicable = true;
    for (Score subScore : subScores) {
      Weight weight = weights.of(subScore).orElseThrow(IllegalStateException::new);
      ScoreValue subScoreValue = calculateIfNecessary(subScore, valueSet);
      if (subScoreValue.isNotApplicable()) {
        continue;
      }
      allNotApplicable = false;
      subScoreValue.weight(weight.value());
      scoreValue.usedValues(subScoreValue);
      scoreSum += weight.value() * subScoreValue.get();
      confidenceSum += weight.value() * subScoreValue.confidence();
      weightSum += weight.value();
    }

    if (allNotApplicable) {
      return scoreValue.makeNotApplicable();
    }

    if (weightSum == 0) {
      throw new IllegalArgumentException("Oh no! Looks like all weights are zero!");
    }

    scoreValue.set(Score.adjust(scoreSum / weightSum));
    scoreValue.confidence(Confidence.adjust(confidenceSum / weightSum));

    return scoreValue;
  }

  /**
   * The score doesn't use any feature directory
   * so that this method returns an empty set.
   *
   * @return An empty set of features.
   */
  @Override
  public final Set<Feature> features() {
    return Collections.emptySet();
  }

  /**
   * Looks for a sub-score by its type.
   *
   * @param clazz The class instance of the sub-score.
   * @param <T> The type of the sub-score
   * @return An {@link Optional} instance with the sub-score.
   */
  public <T> Optional<T> subScore(Class<T> clazz) {
    Objects.requireNonNull(clazz, "Hey! Class can't be null!");
    if (!Score.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException(String.format(
          "I expected an instance of Score but you gave me %s!", clazz.getCanonicalName()));
    }
    for (Score subScore : subScores()) {
      if (subScore.getClass() == clazz) {
        return Optional.of(clazz.cast(subScore));
      }
    }
    return Optional.empty();
  }

  @Override
  public void accept(Visitor visitor) {
    super.accept(visitor);
    for (Score score : subScores) {
      score.accept(visitor);
    }
    for (Weight weight : weights.parameters()) {
      weight.accept(visitor);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof WeightedCompositeScore == false) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    WeightedCompositeScore that = (WeightedCompositeScore) o;
    return Objects.equals(subScores, that.subScores)
        && Objects.equals(weights, that.weights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), subScores, weights);
  }
}
