package fr.naruse.api.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.naruse.api.logging.GlobalLogger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Configuration {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    public static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();
    public static final GlobalLogger.Logger LOGGER = new GlobalLogger.Logger("ConfigurationLoader");

    private File file;
    private String defaultResourceName;
    private boolean loadDefaultResource;
    private Map<String, Object> map = new HashMap<>();

    public Configuration() {
        this("{}");
    }


    public Configuration(File file) {
        this(file, true);
    }

    public Configuration(File file, boolean loadDefaultResource) {
        this(file, file.getName(), loadDefaultResource);
    }

    public Configuration(File file, String defaultResourceName) {
        this(file, defaultResourceName, true);
    }

    private String json;
    public Configuration(String json) {
        this.json = json;

        try {
            this.reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration(Map<String, Object> map) {
        this(GSON.toJson(map));
    }

    private InputStream defaultResourceStream;
    public Configuration(File file, Class clazz, String path) {
        this(file, clazz.getClassLoader().getResourceAsStream(path));
    }

    public Configuration(File file, InputStream defaultResourceStream) {
        this.file = file;
        this.defaultResourceName = "";
        this.loadDefaultResource = true;
        this.defaultResourceStream = defaultResourceStream;

        LOGGER.info("Loading '"+file.getName()+"'...");
        try {
            this.reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("'"+file.getName()+"' loaded");
    }

    public Configuration(File file, String defaultResourceName, boolean loadDefaultResource) {
        this.file = file;
        this.defaultResourceName = defaultResourceName;
        this.loadDefaultResource = loadDefaultResource;

        LOGGER.info("Loading '"+file.getName()+"'...");
        try {
            this.reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("'"+file.getName()+"' loaded");
    }

    public void reload() throws IOException {
        if(this.json != null){
            this.map = GSON.fromJson(json, MAP_TYPE);
            return;
        }

        if(!this.file.exists()){
            this.file.getParentFile().mkdirs();
            this.file.createNewFile();
        }

        FileInputStream baseFileInputStream = new FileInputStream(file);
        InputStreamReader baseInputStreamReader = new InputStreamReader(baseFileInputStream, "UTF-8");
        BufferedReader baseReader = new BufferedReader(baseInputStreamReader);

        Map<String, Object> map = GSON.fromJson(baseReader.lines().collect(Collectors.joining()), MAP_TYPE);
        if(map != null){
            this.map = map;
        }

        baseReader.close();
        baseFileInputStream.close();
        baseFileInputStream.close();

        this.fill();
    }

    public void fill(){
        if(!this.loadDefaultResource) {
            return;
        }
        InputStream inputStream = this.defaultResourceStream != null ? this.defaultResourceStream : Configuration.class.getClassLoader().getResourceAsStream("resources/"+this.defaultResourceName);
        if(inputStream != null) {
            try{
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader reader = new BufferedReader(inputStreamReader);

                String json = reader.lines().collect(Collectors.joining());

                reader.close();
                inputStreamReader.close();
                inputStream.close();

                Map<String, Object> resourceMap = GSON.fromJson(json, MAP_TYPE);

                if(this.map != null){
                    this.fillMap(this.map, resourceMap);
                }

                this.save();
            }catch (Exception e){
                if(e.getMessage().contains("ZipFile")){
                    return;
                }
                e.printStackTrace();
            }
        }
    }

    private void fillMap(Map<String, Object> map, Map<String, Object> resourceMap){
        for (String key : resourceMap.keySet()) {
            if(!map.containsKey(key)){
                map.put(key, resourceMap.get(key));
            }else if(resourceMap.get(key) instanceof Map){
                fillMap((Map<String, Object>) map.get(key), (Map<String, Object>) resourceMap.get(key));
            }
        }
    }

    public <T> T get(String path){
        return (T) this.map.get(path);
    }

    public int getInt(String path){
        Object o = get(path);
        if(o instanceof Double){
            int i = (int) (double) o;
            o = i;
        }
        return (int) o;
    }

    public long getLong(String path){
        Object o = get(path);
        if(o instanceof Double){
            long i = (long) (double) o;
            o = i;
        }
        return (long) o;
    }

    public boolean getBoolean(String path){
        return Boolean.valueOf(get(path).toString());
    }

    public void set(String path, Object o){
        if(o == null){
            this.map.remove(path);
        }else{
            this.map.put(path, o);
        }
    }

    public ConfigurationSection newSection(String path){
        this.map.put(path, new HashMap<>());
        return new ConfigurationSection(path);
    }

    public boolean contains(String path){
        return this.map.containsKey(path);
    }

    public ConfigurationSection getSection(String path){
        return new ConfigurationSection(path);
    }

    public ConfigurationSectionMain getMainSection(){
        return new ConfigurationSectionMain(this);
    }

    public void setClass(Object clazz){
        String json = GSON.toJson(clazz);
        this.map = GSON.fromJson(json, MAP_TYPE);
    }

    public void clear(){
        this.map.clear();
    }

    public <T> T getClassInstance(Class<T> clazz, String path){
        String json = GSON.toJson(getSection(path).getAll());
        return GSON.fromJson(json, clazz);
    }

    public <T> T getClassInstance(Class<T> clazz){
        return GSON.fromJson(this.toJson(), clazz);
    }

    public void save(File file){
        try{
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            OutputStreamWriter fileWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
            if(this.json != null){
                fileWriter.write(this.json);
            }else{
                String json = GSON.toJson(this.map, this.MAP_TYPE);
                fileWriter.write(json);
            }
            fileWriter.flush();
            fileWriter.close();
            fileOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void save(){
        this.save(this.file);
    }

    public File getConfigFile() {
        return file;
    }

    public String toJson() {
        return GSON.toJson(this.map, this.MAP_TYPE);
    }


    public class ConfigurationSection {

        protected String initialPath;
        private ConfigurationSection section;

        public ConfigurationSection(ConfigurationSection section, String initialPath) {
            this(initialPath);
            this.section = section;
        }

        public ConfigurationSection(String initialPath) {
            this.initialPath = initialPath;
        }

        public <T> T get(String path){
            if(this.section != null){
                return (T) (((Map<String, Object>) section.get(initialPath)).get(path));
            }
            return (T) (((Map<String, Object>) map.get(initialPath)).get(path));
        }

        public int getInt(String path){
            if(this.section != null){
                return (int) (double) ((Map<String, Object>) section.get(initialPath)).get(path);
            }
            return (int) (double) ((Map<String, Object>) map.get(initialPath)).get(path);
        }

        public long getLong(String path){
            if(this.section != null){
                return (long) (double) ((Map<String, Object>) section.get(initialPath)).get(path);
            }
            return (long) (double) ((Map<String, Object>) map.get(initialPath)).get(path);
        }

        public void set(String path, Object o){
            if(this.section != null){
                if(o == null){
                    ((Map<String, Object>) section.get(initialPath)).remove(path);
                }else{
                    ((Map<String, Object>) section.get(initialPath)).put(path, o);
                }
            }else{
                if(o == null){
                    ((Map<String, Object>) map.get(initialPath)).remove(path);
                }else{
                    ((Map<String, Object>) map.get(initialPath)).put(path, o);
                }
            }
        }

        public ConfigurationSection newSection(String path){
            if(this.section != null){
                ((Map<String, Object>) section.get(initialPath)).put(path, new HashMap<>());
            }else{
                ((Map<String, Object>) map.get(initialPath)).put(path, new HashMap<>());
            }
            return new ConfigurationSection(this, path);
        }

        public boolean contains(String path){
            if(this.section != null){
                return ((Map<String, Object>) section.get(initialPath)).containsKey(path);
            }
            return ((Map<String, Object>) map.get(initialPath)).containsKey(path);
        }

        public boolean getBoolean(String path){
            return Boolean.valueOf(get(path).toString());
        }

        public ConfigurationSection getSection(String path){
            return new ConfigurationSection(this, path);
        }

        public List<Configuration> getSectionList(String path){
            List<Configuration> list = new ArrayList<>();
            Object sectionObj = this.get(path);
            if(sectionObj instanceof ArrayList){
                ((ArrayList<Map<String, Object>>) sectionObj).forEach(hashMap -> list.add(new Configuration(hashMap)));
            }else{
                return null;
            }
            return list;
        }

        public Map<String, Object> getAll(){
            if(this.section != null){
                return section.get(initialPath);
            }
            return ((Map<String, Object>) map.get(initialPath));
        }

        public <T> T getClassInstance(Class<T> clazz, String path){
            String json = GSON.toJson(getAll(), MAP_TYPE);
            return GSON.fromJson(json, clazz);
        }

        public String toJson(){
            return GSON.toJson(this.getAll(), MAP_TYPE);
        }

        public String getInitialPath() {
            return initialPath;
        }
    }

    public class ConfigurationSectionMain extends ConfigurationSection {

        private final Configuration configuration;

        public ConfigurationSectionMain(Configuration configuration) {
            super(null, null);
            this.configuration = configuration;
        }

        @Override
        public <T> T get(String path) {
            return this.configuration.get(path);
        }

        @Override
        public int getInt(String path) {
            return this.configuration.getInt(path);
        }

        @Override
        public long getLong(String path) {
            return this.configuration.getLong(path);
        }

        @Override
        public void set(String path, Object o) {
            this.configuration.set(path, o);
        }

        @Override
        public ConfigurationSection newSection(String path) {
            return this.configuration.newSection(path);
        }

        @Override
        public boolean contains(String path) {
            return this.configuration.contains(path);
        }

        @Override
        public boolean getBoolean(String path) {
            return this.configuration.getBoolean(path);
        }

        @Override
        public ConfigurationSection getSection(String path) {
            return this.configuration.getSection(path);
        }

        @Override
        public List<Configuration> getSectionList(String path) {
            return super.getSectionList(path);
        }

        @Override
        public Map<String, Object> getAll() {
            return this.configuration.map;
        }

        @Override
        public <T> T getClassInstance(Class<T> clazz, String path) {
            String json = this.configuration.toJson();
            return GSON.fromJson(json, clazz);
        }

        @Override
        public String toJson() {
            return this.configuration.toJson();
        }

        public void setInitialPath(String newInitialPath){
            this.initialPath = newInitialPath;
        }
    }
}
