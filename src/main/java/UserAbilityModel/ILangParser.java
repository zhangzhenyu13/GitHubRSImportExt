package UserAbilityModel;

import java.util.List;

public interface ILangParser {
	/*
	 * parse the code of a kind of language and then return the tags of abilities attached to this code file.
	 */
	List<String> parse(String code);
}
