package ca.phon.ipamap2;

import ca.phon.ui.ipamap.io.IpaGrids;
import ca.phon.ui.ipamap.io.ObjectFactory;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatGrids {

    private IpaGrids grids;

    private final static String GRID_FILE = "chat.xml";

    private ChatGrids() {
        super();
    }

    public IpaGrids loadGrids() {
        ObjectFactory factory = new ObjectFactory();
        try {
            JAXBContext ctx = JAXBContext.newInstance(factory.getClass());
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            grids = (IpaGrids)unmarshaller.unmarshal(
                    IPAGrids.class.getResource(GRID_FILE));
        } catch (JAXBException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
            grids = factory.createIpaGrids();
        }
        return grids;
    }

    public static ChatGrids getInstance() {
        return ChatGridsInstance.INSTANCE;
    }

    private static class ChatGridsInstance {
        private static final ChatGrids INSTANCE = new ChatGrids();
    }

    @Override
    public String toString() {
        return "ChatGrids";
    }

}
