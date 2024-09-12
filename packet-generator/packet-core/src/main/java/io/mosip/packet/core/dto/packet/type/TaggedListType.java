package io.mosip.packet.core.dto.packet.type;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TaggedListType {

	private String value;
	private String[] tags;

}
