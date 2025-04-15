package eu.maveniverse.maven.njord.shared.publisher;

import eu.maveniverse.maven.njord.shared.store.ArtifactStore;
import java.io.IOException;
import java.util.Collection;

public interface ArtifactStoreValidator {
    /**
     * The validation result. Result is valid as long as there are no errors.
     */
    interface ValidationResult {
        default boolean isValid() {
            return error().isEmpty();
        }

        Collection<String> info();

        Collection<String> warning();

        Collection<String> error();
    }

    /**
     * Validator name,
     */
    String name();

    /**
     * Performs the validation.
     */
    ValidationResult validate(ArtifactStore artifactStore) throws IOException;
}
