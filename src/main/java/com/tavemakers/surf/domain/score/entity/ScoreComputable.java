package com.tavemakers.surf.domain.score.entity;

import java.math.BigDecimal;

public interface ScoreComputable {

    BigDecimal getScore();

    BigDecimal updateScore(BigDecimal score);

}
