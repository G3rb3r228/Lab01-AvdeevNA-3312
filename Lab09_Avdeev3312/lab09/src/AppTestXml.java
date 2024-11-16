import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class AppTestXml {

    // Тестирование метода checkName
    @Test
    public void testCheckName_ValidInput() throws Exception {
        JTextField searchField = new JTextField("Espresso");
        App.checkName(searchField);  // Должен пройти без исключений
    }

    @Test
    public void testCheckName_PlaceholderInput_ShouldThrowMyException() {
        JTextField searchField = new JTextField("Поиск");
        Exception exception = assertThrows(App.MyException.class, () -> {
            App.checkName(searchField);
        });
        assertEquals("Вы не ввели название сорта кофе для поиска", exception.getMessage());
    }

    @Test
    public void testCheckName_EmptyInput_ShouldThrowNullPointerException() {
        JTextField searchField = new JTextField("");
        assertThrows(NullPointerException.class, () -> {
            App.checkName(searchField);
        });
    }

    // Тестирование загрузки из XML
    @Test
    public void testLoadFromXml() throws Exception {
        // Создаем временный XML файл с тестовыми данными
        File tempFile = File.createTempFile("test-coffee", ".xml");
        tempFile.deleteOnExit();

        String testXml = """
            <coffees>
                <coffee>
                    <type>Espresso</type>
                    <country>Colombia</country>
                    <roast>Dark</roast>
                    <grade>88</grade>
                </coffee>
                <coffee>
                    <type>Cappuccino</type>
                    <country>Brazil</country>
                    <roast>Medium</roast>
                    <grade>85</grade>
                </coffee>
            </coffees>
            """;

        Files.writeString(Paths.get(tempFile.getPath()), testXml);

        // Создаем пустую модель таблицы
        String[] columnNames = {"Сорт кофе", "Страна произрастания", "Степень обжарки", "Оценка Q-грейдера"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        // Загружаем данные из XML файла
        App.loadTableFromXml(tempFile, model); // Передаем файл и модель таблицы

        // Проверяем данные в модели таблицы
        assertEquals("Espresso", model.getValueAt(0, 0));
        assertEquals("Colombia", model.getValueAt(0, 1));
        assertEquals("Dark", model.getValueAt(0, 2));
        assertEquals("88", model.getValueAt(0, 3));

        assertEquals("Cappuccino", model.getValueAt(1, 0));
        assertEquals("Brazil", model.getValueAt(1, 1));
        assertEquals("Medium", model.getValueAt(1, 2));
        assertEquals("85", model.getValueAt(1, 3));
    }

}