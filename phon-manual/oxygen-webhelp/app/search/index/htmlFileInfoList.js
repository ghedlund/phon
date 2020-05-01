define(function () {
return ["analysis/analyses.html@@@Built-in Analyses@@@Analyses built into Phon offer ready access to general results about different aspects of phonological behaviours at a click. Currently-supported analyses include: Consonants: a series of analyses on...","analysis/analysis_composer.html@@@Custom Analysis using Composer (simple)@@@...","analysis/consonants.html@@@Consonants@@@The Consonants analysis consists of a series of algorithms to determine the accuracy of consonants in singleton and complex syllable onsets and codas as well as in heterosyllabic clusters. Options...","analysis/phone_inventory.html@@@Phone Inventory@@@The Phone Inventory analysis will calculate independent phoneme inventories of the IPA Target and IPA Actual tiers in the sampled data. The report will be composed of the following types of tables...","analysis/phonological_processes.html@@@Phonological Processes@@@...","analysis/pmlu.html@@@Phonological Mean Length of Utterance (PMLU)@@@The PMLU analysis calculates Phonlogical Mean Length of Utterance (PMLU) (Ingram 2002) Expanded Phonological Mean Length of Utterance (ePMLU) (Arias & Lle\u00F3 2013)...","analysis/ppc.html@@@PPC@@@The PPC analysis will calculate the percent phones/consonants/vowels correct in the sampled data. The report will be composed of two types of tables: PPC Summary PPC Listing An example table of...","analysis/specialized/multisyllabic_noninear_analysis.html@@@Multisyllabic Nonlinear Analysis@@@This document describes the Multisyllabic Nonlinear Analysis (MNA) report in Phon. TODO - Information about analysis and citations which will be included at the top of the report The report will...","analysis/specialized/vocalization.html@@@Vocalization@@@...","analysis/specialized/wap.html@@@Word-level Analysis of Polysyllables (WAP)@@@This document outlines the Word-level Analysis of Polysyllables (WAP) report in Phon. The WAP (Masso, 2016) was developed in 2016 and originally published as a supplementary appendix to Masso, McLeod...","analysis/specialized/word_match.html@@@Word Match@@@...","analysis/word_list.html@@@Word List@@@...","chat/main_line.html@@@CHAT Main Line@@@The CHAT main line codes the basic transcription of what a speaker said. The main line includes both pronounced forms and markers. In CHAT the main line starts with an asterisk followed by the speaker...","chat/phontalk.html@@@PhonTalk@@@PhonTalk is an application for converting between Phon project and the Talkbank XML format. Files processed in the application must be generated using the chatter application available at...","getting_started.html@@@Getting Started@@@Phon is a software program that supports the building of textual and phonological data corpora. While it was originally created to support the study of child language development, Phon can be used...","html_fragments/download_links.html@@@@@@...","html_fragments/head.html@@@@@@...","html_fragments/pdf_download.html@@@@@@...","install.html@@@Installing Phon@@@Phon 3.0+ requires a 64-bit operating system. Download required fronts from https://www.phon.ca/downloads/ipafonts.zip. Installer - Download and execution the newest Phon_windows-x64_&lt;version&gt;.exe...","ipa/features.html@@@Listing of phonetic features@@@The following is a listing of all the supported phonetic features. Table 1. Features Name Synonyms Primary Family Secondary Family null diacritic unintelligible unreleased diacritic consonant c vowel...","ipa/ipa.html@@@Listing of IPA Characters@@@The following is a lising of all supported IPA characters along with the glyph unicode value, name, token type and feature set. Table 1. Supported IPA Characters Glyph Unicode Value Name Type Features...","ipa/transcription.html@@@Parsing IPA Transcriptions@@@Description of sandhi...","known_issues.html@@@Known Issues@@@Some common issues with Phon are listed in the table below. To report a problem goto http://github.com/phon-ca/phon/issues. | Issue | Resolution | |--|--| | Phon freezes when loading .wav file in...","misc/Welcome.html@@@@@@...","phonex/boundaries.html@@@Boundaries@@@Description of special boundary markers...","phonex/comb.html@@@comb@@@Description of the comb supplementary matcher...","phonex/comments.html@@@Comments@@@Comments may be inserted in phonex expression using c-style comment syntax. There are two types of comments: General Comment /* ... */ End of Line Comment // ... General comments start with the /*...","phonex/constructs.html@@@Phonex Constructs@@@A summary of all possible phonex constructs...","phonex/diacritic_plugin.html@@@diacritic@@@Description of the phonex diacritic supplementary matcher...","phonex/examples/consonants_examples.html@@@Basic Phone Matching@@@Query for consonants using the base glyph. Examples: | Phonex | Meaning | |--|--| | b | Any consonant with &apos;b&apos; as the base glyph | | d | Any consonant with &apos;d&apos; as the base glyph | Query using feature...","phonex/examples/phones.html@@@Example Phones Queries@@@The following examples are available in Phon from the Phones query window as named queries . // Look-behind &apos;(?&lt;&apos; and match beginning of input &apos;^&apos; // followed by an optional stress marker &apos;\\s?&apos...","phonex/groups.html@@@Groups@@@Phonex allows defining groups by placing any subpattern between the parenthesis - ( and ) - metacharacters. Some reasons to use groups: Repeating subpatterns Extract information for furthur processing...","phonex/intro.html@@@Introduction@@@Phonex is a pattern matching language for IPA transcriptions. It was developed for the Phon application as a method of searching for phones and phone sequences using termonology familiar with...","phonex/phone_matchers.html@@@Phone Matchers@@@Various methods of matching phones using phonex 2.0...","phonex/phonex_introduction.html@@@Introduction@@@Phonex is a pattern matching language for IPA transcriptions. Phonex is used to query IPA transcriptions for sequences of phones based on both segmental and prosodic criteria. Features include: Query...","phonex/plug-ins.html@@@Overview@@@Supplementary matchers in phonex 2.0...","phonex/prefix.html@@@prefix@@@Description of the prefix supplementary matcher...","phonex/quantifiers.html@@@Quantifiers@@@Description of phonex quantifiers...","phonex/sctype.html@@@sctype@@@Description of the phonex sctype supplementary matcher...","phonex/stress.html@@@stress@@@Description of the phonex stress supplementary matcher...","phonex/suffix.html@@@suffix@@@Description of the suffix supplementary matcher...","phonex/tone_plugin.html@@@tone@@@Description of the tone supplementary matcher...","praat_plugin/formant_settings.html@@@Formant Settings@@@Settings for Formant analysis...","praat_plugin/intensity_settings.html@@@Intensity Settings@@@Configuring the intensity contour...","praat_plugin/pitch_settings.html@@@Pitch Settings@@@Configuring the pitch contour...","praat_plugin/spectrogram_settings.html@@@Spectrogram Settings@@@Configuring the spectrogram...","prefs/preferences.html@@@Preferences@@@Change the default Dictionary Language for use within sessions...","project_manager/corpora.html@@@Corpus List@@@Rename a corpus in your project...","project_manager/project_manager.html@@@Project Manager@@@In the event of display issues in Target Syllables or Actual Syllables in the Syllabification &\n                        Alignment view, it may be necessary to reset the syllabification of IPA Target and IPA Actual transcriptions...","project_manager/sessions.html@@@Session List@@@Open a session in the default mode...","query/common_query_options.html@@@Common Query Paramters@@@Limit the results based on position or an expression defining the word group...","query/data_tiers_query.html@@@Data Tiers@@@The Data Tiers query allows searching within any record tier. Figure 1 : Data Tiers Query Enter the name of the tier in which you would like to search in the Tier name field. Choose an Expression Type...","query/deletions_query.html@@@Deletions@@@A special case of the Phones query which will search for phone deletions in phone alignment...","query/epenthesis_query.html@@@Epenthesis@@@A special case of the Phones query which will search for epenthesis within phone alignment...","query/phones_query.html@@@Phones@@@The Phones query is used to query data contained within IPA Target and IPA Actual tiers. Figure 1 : Phones Query Search Tier: IPA Target or IPA Actual. Search by: Group, Word and optionally by...","query/query_and_report_wizard.html@@@Query and Report Wizard@@@The query and report wizard is displayed when executing any query. The wizard has three main steps: Query - enter query settings and execute query. Report Composer - choose and configure report...","query/segmental_relations_query.html@@@Segmental Relations@@@Look for segmental relations within phone alignment. Figure 1 : Segmental Relations Query...","query/view_result_set.html@@@View Result Set@@@View a Result Set from a query...","redirect.html@@@@@@...","report/acoustic_data_reports/acoustic_data_reports.html@@@Acoustic Data Reports@@@Acoustic data reports print acoustic information for each query result. All acoustic data reports require identification of one or more intervals in the audio for analysis. Intervals are selected...","report/acoustic_data_reports/duration_report.html@@@Duration@@@Print duration of each selected TextGrid interval. Report table will have the following columns: Session Speaker Age Record # Result/Tier Name Additional tiers added to query results Start Time (s)...","report/acoustic_data_reports/formants_report.html@@@Formants@@@Display formant values at various points within each selected TextGrid interval. Session Speaker Age Record # IPA Actual Start Time End Time F110 F120 F130 F140 F150 F160 F170 F180 F190 F210 F220 F230...","report/acoustic_data_reports/intensity_report.html@@@Intensity@@@Display intensity values (dB) at various positions within each selected TextGrid interval. Session Speaker Age Record # IPA Actual Start Time End Time I10(dB) I20(dB) I30(dB) I40(dB) I50(dB) I60(dB)...","report/acoustic_data_reports/pitch_report.html@@@Pitch@@@Display pitch values at various points within each selected TextGrid interval. Session Speaker Age Record # IPA Actual Start Time End Time P10(Hz) P20(Hz) P30(Hz) P40(Hz) P50(Hz) P60(Hz) P70(Hz)...","report/acoustic_data_reports/spectral_moments_report.html@@@Spectral Moments@@@Display Center of Gravity, Standard Deviation, Kurtosis, Skewness for each selected TextGrid interval. Session Speaker Age Record # IPA Actual Start Time(s) End Time(s) Center of Gravity Standard...","report/acoustic_data_reports/vot_report.html@@@Voice Onset Time (VOT)@@@The Voice Onset Time (VOT) report calculates VOT for each query result. Under the setup described below: Segment duration for stops is calculated from stop closure to stop release. While closure is...","report/aggregate_report.html@@@Aggregate@@@Create an aggregate inventory of results. Each session sampled in the query will have a column in the inventory table. Orthography Catootje.1_11_09 Catootje.2_00_19 aan 0 1 aap 1 0 aardbei 1 2 allebei...","report/inventory_by_participant_report.html@@@Inventory by Participant@@@Text...","report/inventory_by_session_report.html@@@Inventory by Session@@@Display an inventory of results for each sampled session. A separate table is displayed for each session. Orthography Catootje.1_11_09 aap 1 aardbei 1 allebei 1 ander 2 andere 1 appel 3 baby 1 bal 1...","report/listing_by_session_report.html@@@Listing by Session (with optional tier data)@@@Text...","report/phones/phone_accuracy_report.html@@@Phone Accuracy@@@...","report/phones/phone_similarity_report.html@@@Phone Similarity@@@Phone similarity measures how similar two phones or strings of phones are within a target-actual aligned pair based on the number of descriptive phonological matchings divided by the maximal number of...","report/phones/transcript_variability_report.html@@@Transcript Variability@@@Session # Repeated IPA Target # All Correct # One or More Correct # Same Error # Different Errors Avg Distance Catootje.1_11_09 36 2 5 8 26 2.11 Catootje.2_00_19 35 7 7 16 12 1.16...","report/query_information_report.html@@@Query Information@@@Display query name and parameters...","report/table.html@@@Table (all results in one table)@@@For more information please submit your query to our discussion group (no membership required) at phon@googlegroups.com...","report/table_by_participant_report.html@@@Table by Participant@@@This report will list each query result in a table. Results from all sampled sessions will be included in a single table. Any tiers specified in the &apos;Aligned Group&apos;, &apos;Aligned Word&apos;, &apos;Add aligned...","report/table_by_session_report.html@@@Table by Session@@@This report will list each query result in a table. A separate table will be displayed for each sampled session. Any tiers specified in the &apos;Aligned Group&apos;, &apos;Aligned Word&apos;, &apos;Add aligned group&apos;, and...","session_editor/blind_transcription.html@@@Blind Transcription@@@Due to the subjective nature of phonetic transcription (where measurement and verification of all relevant segments may not be feasible), Phon has a built-in system for performing multiple-blind...","session_editor/editor_keystrokes.html@@@Keyboard Shortcuts@@@Global Keystrokes The following table outlines editor keystrokes which are globally available. Command Mac Windows Notes Save CMD+S CTRL+S New Record CMD+N CTRL+N The new record will be added after...","session_editor/editor_layouts.html@@@Editor Layouts@@@Save currently visible Session Editor layout for future use...","session_editor/editor_media.html@@@Media@@@Sessions in Phon usually consist of a media recording (either audio or video) coupled with transcriptions of utterances from the recording and separated into records. Each record can be associated via...","session_editor/editor_search.html@@@Search@@@Run a quick search within your session and navigate to records containing results...","session_editor/editor_views.html@@@Views@@@Information in each session is displayed in a series of small windows within the Session Editor, called views. There are several types of views...","session_editor/find_and_replace_view.html@@@Find and Replace@@@The Find & Replace view allows for advance find and replace actions within record tiers. Figure 1 : Find & Replace View From Session Editor window: Select the View &gt; Find & Replace menu item. The Find...","session_editor/ipa_lookup_view.html@@@IPA Lookup@@@Automatically transcribe a session using an available built-in dictionary...","session_editor/ipa_validation_view.html@@@IPA Validation@@@Validate completed blind transcriptions...","session_editor/media_player_view.html@@@Media Player@@@Jump to a specific playback time (either user-specified, at the end of segmented media, or at the end of the last segment for a certain speaker...","session_editor/participants.html@@@Participants@@@In the Phon Session Editor , the Participant(s) in a session may be specified and records may be associated with individual participants. &apos;Participant&apos;, in this case means a speaker in a research...","session_editor/record_data_view.html@@@Record Data@@@Add a new word group to a record...","session_editor/records.html@@@Records@@@Add a new record to an existing session...","session_editor/session_editor.html@@@Session Editor@@@...","session_editor/session_information_view.html@@@Session Information@@@Edit a session&apos;s date to reflect the date of its associated media recording...","session_editor/speech_analysis_view.html@@@Speech Analysis@@@Adjust segment start and end times to refine segments...","session_editor/syllabification_and_alignment_view.html@@@Syllabification & Alignment@@@Check to ensure proper syllabification of IPA Target and IPA Actual phones...","session_editor/tier_management_view.html@@@Tier Management@@@Create a new user-defined tier...","session_editor/timeline_view.html@@@Timeline@@@The Timeline view displays the waveform for the session audio (when the audio file is available) as well as associated record data for each selected participant along a horizontal timeline. The...","tools/ipamap.html@@@IPA Map@@@About the IPA Map in Phon...","tools/iso_langauge_codes.html@@@ISO Language Codes@@@...","tools/phonshell.html@@@PhonShell@@@PhonShell is a plug-in introduced with Phon 2.0 which provides a scripting environment for Phon . PhonShell is available from the Tools windows menu. The PhonShell console is associated to the window...","welcome/welcome_window.html@@@Welcome Window@@@The Welcome window is the first window displayed when opening Phon . It is divided into three sections: Actions, Workspace, and Recent Projects. Figure 1 : Welcome Window From the Workspace window..."];
});