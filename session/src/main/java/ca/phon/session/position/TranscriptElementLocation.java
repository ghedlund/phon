/*
 * Copyright (C) 2005-2020 Gregory Hedlund & Yvan Rose
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.phon.session.position;

/**
 * Represents a specific character location withing a transcript element.  If the transcript element is
 * a record, tier will be the name of the specific tier.  If the transcript element is a comment, the
 * tier name will be the type of comment, if the transcript element is a gem, tier will be the type of gem.
 *
 * @param transcriptElementIndex
 * @param tier
 * @param charPosition
 */
public record TranscriptElementLocation(int transcriptElementIndex, String tier, int charPosition) {
    public boolean valid() {
        return transcriptElementIndex >= -1 && tier != null && charPosition >= 0;
    }

    public int compareTo(TranscriptElementLocation other) {
        int retVal = Integer.compare(transcriptElementIndex, other.transcriptElementIndex);
        if(retVal == 0) {
            retVal = tier.compareTo(other.tier);
        }
        if(retVal == 0) {
            retVal = Integer.compare(charPosition, other.charPosition);
        }
        return retVal;
    }
}
