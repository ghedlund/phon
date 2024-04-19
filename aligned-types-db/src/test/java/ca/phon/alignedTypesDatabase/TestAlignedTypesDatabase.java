package ca.phon.alignedTypesDatabase;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.util.*;

@RunWith(JUnit4.class)
public class TestAlignedTypesDatabase {

	private final static String TYPEMAP_CSV_FILE = "typeMap.csv";

	private void removeQuotes(String[] row) {
		for(int i = 0; i < row.length; i++) {
			row[i] = row[i].substring(1, row[i].length()-1);
		}
	}

	private AlignedTypesDatabase loadDatabase() throws IOException {
		final AlignedTypesDatabase database = new AlignedTypesDatabase();

		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(TYPEMAP_CSV_FILE), "UTF-8"))) {
			String line = reader.readLine();
			final String[] colNames = line.split(",");
			removeQuotes(colNames);

			final List<String[]> rows = new ArrayList<>();
			while((line = reader.readLine()) != null) {
				final String[] row = line.split(",");
				rows.add(row);
			}
			Collections.shuffle(rows);

			for(String[] row:rows) {
				removeQuotes(row);

				final Map<String, String> typeMap = new LinkedHashMap<>();
				for(int i = 0; i < colNames.length; i++) {
					typeMap.put(colNames[i], row[i]);
					database.addAlignedTypes(typeMap);
				}
			}
		}

		return database;
	}

	private Set<String> loadUniqueTypes(int column) throws IOException {
		final Set<String> retVal = new LinkedHashSet<>();
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(TYPEMAP_CSV_FILE), "UTF-8"))) {
			String line = reader.readLine();

			while ((line = reader.readLine()) != null) {
				final String[] row = line.split(",");
				removeQuotes(row);
				if(column < 0) {
					for (String val : row) {
						retVal.add(val);
					}
				} else {
					retVal.add(row[column]);
				}
			}
		}
		return retVal;
	}

	private void checkDatabase(AlignedTypesDatabase database) throws IOException {
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(TYPEMAP_CSV_FILE), "UTF-8"))) {
			String line = reader.readLine();
			final String[] colNames = line.split(",");
			removeQuotes(colNames);

			while((line = reader.readLine()) != null) {
				final String[] row = line.split(",");
				removeQuotes(row);

				final Map<String, String> typeMap = new LinkedHashMap<>();
				for(int i = 0; i < colNames.length; i++) {
					typeMap.put(colNames[i], row[i]);

					Assert.assertTrue(database.hasAlignedTypes(typeMap));
				}
			}
		}
	}

	@Test
	public void roundTripTest() throws IOException, ClassNotFoundException {
		final AlignedTypesDatabase db = loadDatabase();
		checkDatabase(db);

		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final ObjectOutputStream oout = new ObjectOutputStream(bout);
		oout.writeObject(db);
		oout.flush();
		oout.close();

		final ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		final ObjectInputStream oin = new ObjectInputStream(bin);
		final AlignedTypesDatabase rtDb = (AlignedTypesDatabase) oin.readObject();
		oin.close();

		checkDatabase(rtDb);
	}

	@Test
	public void testRemoveType() throws IOException {
		final AlignedTypesDatabase db = loadDatabase();
		final String[] tierNames = {"Orthography","IPA Target","IPA Actual","MorphCat","Language"};
		final String[] types = {"Amerika","ˌaˈmeʀika","ʔmˈmeːˌka","name","nld"};
		final String[] modifiedTypes = {null,"ˌaˈmeʀika","ʔmˈmeːˌka","name","nld"};

		Assert.assertTrue(db.hasAlignedTypes(tierNames, types));
		db.removeType(types[0]);
		Assert.assertTrue(!db.typeExists(types[0]));
		Assert.assertTrue(!db.hasAlignedTypes(tierNames, types));
		Assert.assertTrue(db.hasAlignedTypes(tierNames, modifiedTypes));
	}

	@Test
	public void testTypeIterator() throws IOException {
		final AlignedTypesDatabase db = loadDatabase();
		final Set<String> uniqueTypes = loadUniqueTypes(-1);
		final Iterator<String> typeItr = db.typeIterator();
		int cnt = 0;
		while(typeItr.hasNext()) {
			final String type = typeItr.next();
			Assert.assertTrue(uniqueTypes.contains(type));
			++cnt;
		}
		Assert.assertEquals(uniqueTypes.size(), cnt);
	}

	@Test
	public void testTypeIteratorWithFilter() throws IOException {
		final AlignedTypesDatabase db = loadDatabase();
		final Set<String> uniqueOrthos = loadUniqueTypes(0);
		final Iterator<String> typeItr = db.typeIterator((type) -> db.typeExistsInTier(type, "Orthography"));
		int cnt = 0;
		while(typeItr.hasNext()) {
			final String type = typeItr.next();
			if(type.isEmpty()) continue;
			Assert.assertTrue(uniqueOrthos.contains(type));
			++cnt;
		}
		Assert.assertEquals(uniqueOrthos.size(), cnt);
	}

	@Test
	public void testTypeIteratorReset() throws IOException {
		final AlignedTypesDatabase db = loadDatabase();
		final TypeIterator itr = db.typeIterator();

		Assert.assertTrue(itr.hasNext());
		final String firstString = itr.next();
		Assert.assertTrue(itr.hasNext());
		final String secondString = itr.next();
		Assert.assertTrue(itr.hasNext());
		final String thirdString = itr.next();

		itr.reset();
		Assert.assertTrue(itr.hasNext());
		Assert.assertEquals(firstString, itr.next());

		// skip second
		itr.continueFrom(secondString);
		Assert.assertTrue(itr.hasNext());
		Assert.assertEquals(thirdString, itr.next());
	}

}
