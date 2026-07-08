package com.tavemakers.surf.domain.score.domain.entity;

import java.math.BigDecimal;

public interface ScoreComputable {

    BigDecimal getScore();

    BigDecimal updateScore(BigDecimal score);

}
