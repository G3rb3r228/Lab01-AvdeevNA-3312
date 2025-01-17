import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Objects;
import java.awt.event.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.*;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.*;
import net.sf.jasperreports.export.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;



public class App {
    private static final Logger log = Logger.getLogger(App.class);

    public static void printReportMultithreaded(String xmlFilePath, String templatePath, String outputPath, String format) {

        log.info("Запуск многопоточной обработки для генерации отчета.");

        CountDownLatch dataLoaded = new CountDownLatch(1);
        CountDownLatch dataEdited = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Поток для загрузки данных
        executor.submit(() -> {
            log.info("Начало загрузки данных из файла: " + xmlFilePath);
            try {
                JRXmlDataSource xmlDataSource = new JRXmlDataSource(new File(xmlFilePath));
                log.info("Данные успешно загружены из файла: " + xmlFilePath);
                dataLoaded.countDown();
            } catch (JRException e) {
                log.severe("Ошибка при загрузке данных: " + e.getMessage());
                JOptionPane.showMessageDialog(null, "Ошибка при загрузке данных: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Поток для редактирования данных
        executor.submit(() -> {
            try {
                log.info("Ожидание завершения загрузки данных.");
                dataLoaded.await();
                log.info("Загрузка данных завершена. Начало редактирования данных.");
                // Реализуйте редактирование данных здесь
                log.info("Редактирование данных завершено.");
                dataEdited.countDown();
            } catch (InterruptedException ex) {
                log.warn("Редактирование данных прервано: " + ex.getMessage());
            }
        });

        // Поток для генерации отчета
        executor.submit(() -> {
            try {
                log.info("Ожидание завершения редактирования данных.");
                dataEdited.await();
                log.info("Редактирование данных завершено. Начало генерации отчета.");
                printReport(xmlFilePath, templatePath, outputPath, format);
                log.info("Отчет успешно сгенерирован. Выходной файл: " + outputPath);
            } catch (InterruptedException ex) {
                log.warn("Генерация отчета прервана: " + ex.getMessage());
            }
        });

        executor.shutdown();
        try {
            if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                log.info("Все задачи завершены.");
            }
        } catch (InterruptedException ex) {
            log.warn("Ожидание завершения задач было прервано: " + ex.getMessage());
        }
    }
    
    public static void printReportMultithreaded(String xmlFilePath, String templatePath, String outputPath, String format) {
        // Создаем объекты для синхронизации
        CountDownLatch dataLoaded = new CountDownLatch(1);
        CountDownLatch dataEdited = new CountDownLatch(1);

        // Создаем пул потоков
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Запуск потока для загрузки данных
        executor.submit(() -> {
            try {
                // Загрузка данных из XML
                JRXmlDataSource xmlDataSource = new JRXmlDataSource(new File(xmlFilePath));
                // Сообщаем, что данные загружены
                dataLoaded.countDown();
            } catch (JRException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка при загрузке данных: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Запуск потока для редактирования данных
        executor.submit(() -> {
            try {
                dataLoaded.await(); // Ожидаем завершения загрузки данных
                // Редактирование данных (реализуйте в соответствии с вашими требованиями)

                dataEdited.countDown(); // Сообщаем, что данные редактированы
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        // Запуск потока для генерации отчета
        executor.submit(() -> {
            try {
                dataEdited.await(); // Ожидаем завершения редактирования данных
                // Генерация отчета
                printReport(xmlFilePath, templatePath, outputPath, format);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public static void printReport(String xmlFilePath, String templatePath, String outputPath, String format) {
        try {
            // Загружаем XML-источник данных
            JRXmlDataSource xmlDataSource = new JRXmlDataSource(new File(xmlFilePath), "/rootElement"); // Замените "rootElement" на корневой элемент XML

            // Компилируем шаблон отчета (.jrxml)
            JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);

            // Параметры отчета (можно оставить пустым, если не используются)
            Map<String, Object> parameters = new HashMap<>();

            // Заполнение отчета
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, xmlDataSource);

            // Сохранение отчета в зависимости от указанного формата
            if ("pdf".equalsIgnoreCase(format)) {
                // Экспорт в PDF
                JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
                JOptionPane.showMessageDialog(null, "Отчет успешно сохранен в PDF: " + outputPath, "Успех", JOptionPane.INFORMATION_MESSAGE);
            } else if ("xlsx".equalsIgnoreCase(format)) {
                // Экспорт в Excel (XLSX)
                JRXlsxExporter exporter = new JRXlsxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath));

                SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
                configuration.setOnePagePerSheet(false);
                configuration.setDetectCellType(true);
                exporter.setConfiguration(configuration);

                exporter.exportReport();
                JOptionPane.showMessageDialog(null, "Отчет успешно сохранен в XLSX: " + outputPath, "Успех", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Неизвестный формат: " + format, "Ошибка", JOptionPane.ERROR_MESSAGE);
            }

        } catch (JRException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка при генерации отчета: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }



    /**
     * Initializes and displays the Coffeeshop application window.
     * The window includes:
     * - A panel with buttons.
     * - The search field.
     * - A table for displaying data about the coffee shop.
     */
    public static void CoffeeShop(){
        // Создание главного окна приложения
        JFrame frame = new JFrame("Coffee Shop");

        // Настройка действия при закрытии окна
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 600); // Установка размера окна

        // Настройка цвета фона окна
        frame.getContentPane().setBackground(new Color(111, 78, 55));

        // Главная панель, которая содержит кнопки и панель поиска
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout()); // Использует BorderLayout для упорядочивания кнопок и поиска

        // Панель для кнопок
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(new Color(30, 2, 2));

        // Массивы с названиями и иконками для кнопок
        String[] icons = {"save", "open", "add", "edit", "bin", "print"};
        String[] buttonsName = {"Сохранить", "Открыть", "Добавить", "Редактировать", "Удалить", "Печать списка"};
        JButton[] buttons = new JButton[icons.length];

        // Добавляем кнопки на панель кнопок
        for (int i = 0; i < icons.length; i++) {
            ImageIcon iconImage = new ImageIcon(new ImageIcon(Objects.requireNonNull(App.class.getResource("/icons/" + icons[i] + ".png")))
                    .getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH));
            buttons[i] = new JButton(buttonsName[i], iconImage);
            buttonPanel.add(buttons[i]);
        }


        /**
         * Обработчик события нажатия на кнопку "Удалить".
         * При нажатии на эту кнопку выполняется удаление выбранного элемента.
         * Логика удаления в данном примере не реализована.
         * Необходимо добавить код для удаления соответствующего элемента
         * (например, строки из таблицы, объекта из списка и т.д.).
         * После успешного удаления выводится диалоговое окно с сообщением.
         */
        buttons[4].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Элемент удалён");
            }
        });


        // Создание панели для поиска
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout(FlowLayout.RIGHT)); // Выравниваем по правому краю
        searchPanel.setBackground(new Color(30, 2, 2));

        // Поле для ввода текста поиска
        JTextField searchField = new JTextField(15);
        JButton searchButton = new JButton("Поиск"); // Кнопка для поиска


        /**
         * Обработчик события нажатия на кнопку поиска.
         * При нажатии на кнопку запускается поиск по таблице по введенному тексту.
         * Текст для поиска берется из поля `searchField`.
         * После выполнения поиска выводится диалоговое окно с результатами.
         */
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    checkName(searchField); // Вызываем функцию проверки

                    // Пример вывода результата поиска (замените на свою логику)
                    JOptionPane.showMessageDialog(null, "Найденные результаты: " + searchField.getText());
                } catch (NullPointerException ex) {
                    JOptionPane.showMessageDialog(null, "Вы не ввели текст для поиска");
                } catch (MyException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage());
                }
            }
        });

        // Добавляем текст плейсхолдера в поле поиска
        searchField.setText("Поиск");
        searchField.setForeground(Color.GRAY); // По умолчанию серый текст

        // Добавляем FocusListener для обработки фокуса поля
        /**
         * Добавляет обработчик событий для текстового поля поиска.
         * - При получении фокуса текст становится черным, и плейсхолдер исчезает.
         * - При потере фокуса, если поле пустое, плейсхолдер возвращается.
         * Использует FocusAdapter — адаптерный класс с пустыми реализациями методов интерфейса FocusListener,
         * включающего focusGained() и focusLost().
         */
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            /**
             * Метод срабатывает, когда текстовое поле поиска получает фокус.
             * - Если текстовое поле содержит "Поиск" (плейсхолдер), этот текст удаляется и цвет текста изменяется на черный.
             *
             * @param e - событие фокуса.
             */
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Поиск")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK); // Цвет текста черный при вводе
                }
            }

            @Override
            /**
             * Метод срабатывает, когда текстовое поле поиска теряет фокус.
             * - Если поле пустое, оно заполняется текстом плейсхолдера "Поиск", а цвет текста изменяется на серый.
             *
             * @param e - событие фокуса.
             */
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(Color.GRAY);
                    searchField.setText("Поиск"); // Возвращаем плейсхолдер
                }
            }
        });

        // Добавляем поле поиска и кнопку в панель поиска
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Добавляем панели кнопок и поиска в верхнюю панель
        topPanel.add(buttonPanel, BorderLayout.CENTER);
        topPanel.add(searchPanel, BorderLayout.EAST); // Панель поиска справа

        // Создание таблицы для отображения данных
        Object[][] data = {
                {"", "", "", ""},
                {"", "", "", ""},
                {"", "", "", ""},
                {"", "", "", ""},
                {"", "", "", ""},
                {"", "", "", ""}
        };

        String[] columnNames = {"Сорт кофе", "Страна произрастания", "Степень обжарки", "Оценка Q-грейдера"};

        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        JTable table = new JTable(model); // Таблица с пустыми данными

        table.setBackground(new Color(164, 127, 102));
        table.setFillsViewportHeight(true); // Растягиваем таблицу на всю доступную высоту
        table.setFont(new Font("Arial", Font.PLAIN, 14));

        // Устанавливаем цвет разделителей
        table.setShowGrid(true);
        table.setGridColor(Color.BLACK); // например, устанавливаем красный цвет

        // Настраиваем заголовок таблицы
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(30, 2, 2));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Arial", Font.BOLD, 16));


        // Обработчик события для кнопки "Сохранить"

        buttons[0].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Получаем модель таблицы
                DefaultTableModel model = (DefaultTableModel) table.getModel();

                // Создаем новый XML-документ
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document document = builder.newDocument();


                    // Корневой элемент
                    Element coffees = document.createElement("coffees");
                    document.appendChild(coffees);

                    // Проходим по строкам таблицы и создаем элементы coffee
                    for (int i = 0; i < model.getRowCount(); i++) {
                        Element coffee = document.createElement("coffee");
                        coffees.appendChild(coffee);

                        // Получаем данные из строки таблицы и проверяем на null
                        String type = (String) model.getValueAt(i, 0);
                        String country = (String) model.getValueAt(i, 1);
                        String roast = (String) model.getValueAt(i, 2);
                        String grade = (String) model.getValueAt(i, 3);

                        // Пропускаем строку, если все значения пустые (можно настроить это условие)
                        if (type == null && country == null && roast == null && grade == null) {
                            continue;
                        }

                        // Создаем элементы и добавляем контент
                        Element typeElement = document.createElement("type");
                        typeElement.appendChild(document.createTextNode(type != null ? type : ""));
                        coffee.appendChild(typeElement);

                        // Аналогично создаем элементы для country, roast и grade
                        Element countryElement = document.createElement("country");
                        countryElement.appendChild(document.createTextNode(country != null ? country : ""));
                        coffee.appendChild(countryElement);

                        // ... (аналогично для roast и grade)
                        Element roastElement = document.createElement("roast");
                        roastElement.appendChild(document.createTextNode(roast != null ? roast : ""));
                        coffee.appendChild(roastElement);

                        Element gradeElement = document.createElement("grade");
                        gradeElement.appendChild(document.createTextNode(grade != null ? grade : ""));
                        coffee.appendChild(gradeElement);

                        coffees.appendChild(coffee);
                    }

                    // Сохраняем XML-документ в файл
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    DOMSource source = new DOMSource(document);


                    JFileChooser fileChooser = new JFileChooser();
                    int userChoice = fileChooser.showSaveDialog(frame);
                    if (userChoice == JFileChooser.APPROVE_OPTION)
                    {
                        File file = fileChooser.getSelectedFile();
                        StreamResult result = new StreamResult(file);
                        transformer.transform(source, result);
                        JOptionPane.showMessageDialog(null, "Данные успешно сохранены в XML файл");
                    }
                } catch (ParserConfigurationException | TransformerException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Ошибка при сохранении в XML файл: " + ex.getMessage());
                }
            }
        });


        buttons[1].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Files", "xml");
                        fileChooser.setFileFilter(filter);

                int returnVal = fileChooser.showOpenDialog(frame);

                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();

                    try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document document = builder.parse(file);
                        Element root = document.getDocumentElement();

                        // Предполагаем, что структура XML следующая:
                        // <coffees>
                        //   <coffee>
                        //     <type>Арабика</type>
                        //     <country>Бразилия</country>
                        //     <roast>Средняя</roast>
                        //     <grade>85</grade>
                        //   </coffee>
                        //   ...
                        // </coffees>

                        NodeList coffeeNodes = root.getElementsByTagName("coffee");
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        model.setRowCount(0); // Очищаем таблицу

                        for (int i = 0; i < coffeeNodes.getLength(); i++) {
                            Element coffee = (Element) coffeeNodes.item(i);
                            String type = coffee.getElementsByTagName("type").item(0).getTextContent();
                            String country = coffee.getElementsByTagName("country").item(0).getTextContent();
                            String roast = coffee.getElementsByTagName("roast").item(0).getTextContent();
                            String grade = coffee.getElementsByTagName("grade").item(0).getTextContent();
                            model.addRow(new Object[]{type, country, roast, grade});
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Ошибка при чтении XML-файла: " + ex.getMessage());
                    }
                }
            }
        });

        AtomicReference<String> format = new AtomicReference<>("pdf"); // Установим значение по умолчанию

        buttons[5].addActionListener(e -> {
            // Диалог выбора XML-файла
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("XML files", "xml"));
            fileChooser.setDialogTitle("Выберите XML-файл");
            int xmlResult = fileChooser.showOpenDialog(null);

            if (xmlResult == JFileChooser.APPROVE_OPTION) {
                String xmlFilePath = fileChooser.getSelectedFile().getAbsolutePath();

                // Диалог выбора шаблона отчета
                fileChooser.resetChoosableFileFilters();
                fileChooser.setDialogTitle("Выберите шаблон отчета (.jrxml)");
                int templateResult = fileChooser.showOpenDialog(null);

                if (templateResult == JFileChooser.APPROVE_OPTION) {
                    String templatePath = fileChooser.getSelectedFile().getAbsolutePath();

                    // Диалог выбора места сохранения отчета
                    fileChooser.resetChoosableFileFilters();
                    fileChooser.setDialogTitle("Сохранить отчет как...");
                    int outputResult = fileChooser.showSaveDialog(null);

                    if (outputResult == JFileChooser.APPROVE_OPTION) {
                        String outputPath = fileChooser.getSelectedFile().getAbsolutePath();

                        // Добавляем расширение файла, если его нет
                        if (!outputPath.toLowerCase().endsWith(".pdf") && !outputPath.toLowerCase().endsWith(".xlsx")) {
                            outputPath += "." + format.get();
                        }

                        // Проверяем, что все пути не null
                        if (xmlFilePath != null && templatePath != null && outputPath != null) {
                            printReport(xmlFilePath, templatePath, outputPath, format.get());
                        } else {
                            JOptionPane.showMessageDialog(null, "Ошибка: один из путей не задан!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        // Добавляем таблицу в JScrollPane для прокрутки
        JScrollPane scrollPane = new JScrollPane(table);

        // Добавляем верхнюю панель и таблицу в окно
        frame.add(topPanel, BorderLayout.NORTH); // Добавляем объединённую верхнюю панель
        frame.add(scrollPane, BorderLayout.CENTER); // Таблица в центре окна

        // Делаем окно видимым
        frame.setVisible(true);



    }



    private static class MyException extends Exception {
        public MyException() {
            super ("Вы не ввели название сорта кофе для поиска");
        }}

    private static void checkName(JTextField cName) throws MyException, NullPointerException {
        String sName = cName.getText();
        if (sName.contains("Поиск")) throw new MyException();
        if (sName.isEmpty()) throw new NullPointerException();
    }


    /**
     * Laboratory work №7.
     *
     * @author Avdeev Nikita;
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::CoffeeShop);
    }
}
