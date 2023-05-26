package jhi.pickamix.server.pojo;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@ToString
public class TrialMeasure
{
	private Integer id;
	private String name;
	private String value;
}
