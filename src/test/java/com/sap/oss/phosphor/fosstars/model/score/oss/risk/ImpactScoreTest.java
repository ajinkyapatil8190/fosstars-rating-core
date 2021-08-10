package com.sap.oss.phosphor.fosstars.model.score.oss.risk;

import static com.sap.oss.phosphor.fosstars.TestUtils.DELTA;
import static com.sap.oss.phosphor.fosstars.model.feature.Impact.HIGH;
import static com.sap.oss.phosphor.fosstars.model.feature.Impact.LOW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sap.oss.phosphor.fosstars.model.Confidence;
import com.sap.oss.phosphor.fosstars.model.Score;
import com.sap.oss.phosphor.fosstars.model.feature.EnumFeature;
import com.sap.oss.phosphor.fosstars.model.feature.Impact;
import com.sap.oss.phosphor.fosstars.model.value.EnumValue;
import com.sap.oss.phosphor.fosstars.model.value.ScoreValue;
import com.sap.oss.phosphor.fosstars.util.Json;
import com.sap.oss.phosphor.fosstars.util.Yaml;
import java.io.IOException;
import org.junit.Test;

public class ImpactScoreTest {

  private static final EnumFeature<Impact> IMPACT_FEATURE
      = new EnumFeature<>(Impact.class, "Test impact");

  private static final ImpactScore SCORE = new ImpactScore("Test impact score", IMPACT_FEATURE);

  @Test
  public void testJsonSerialization() throws IOException {
    ImpactScore clone = Json.read(Json.toBytes(SCORE), ImpactScore.class);
    assertTrue(SCORE.equals(clone) && clone.equals(SCORE));
    assertEquals(SCORE.hashCode(), clone.hashCode());
  }

  @Test
  public void testYamlSerialization() throws IOException {
    ImpactScore clone = Yaml.read(Yaml.toBytes(SCORE), ImpactScore.class);
    assertEquals(clone, SCORE);
  }

  @Test
  public void testCalculate() {
    ScoreValue scoreValue = SCORE.calculate(new EnumValue<>(IMPACT_FEATURE, LOW));
    assertFalse(scoreValue.isUnknown());
    assertFalse(scoreValue.isNotApplicable());
    assertTrue(Score.INTERVAL.contains(scoreValue.get()));
    assertEquals(scoreValue.confidence(), Confidence.MAX, DELTA);

    assertTrue(SCORE.calculate(IMPACT_FEATURE.unknown()).isUnknown());
  }

  @Test
  public void testScoreValueSerialization() throws IOException {
    ScoreValue scoreValue = SCORE.calculate(new EnumValue<>(IMPACT_FEATURE, HIGH));
    ScoreValue clone = Json.read(Json.toBytes(scoreValue), ScoreValue.class);
    assertTrue(scoreValue.equals(clone) && clone.equals(scoreValue));
    assertEquals(scoreValue.hashCode(), clone.hashCode());
  }
}