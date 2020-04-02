package orm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import annotations.Column;
import annotations.Entity;
import annotations.MappedClass;
import annotations.Param;
import annotations.Select;
import dao.BasicMapper;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;

public class MyORM 
{	
	
	HashMap<Class, Class> entityToMapperMap = new HashMap<Class, Class>();
	
	
	public void init() throws Exception
	{
		// scan all mappers -- @MappedClass
		scanMappers();		
		
		// scan all the entities -- @Entity
		scanEntities();
				
		// create all entity tables
		createTables();

	}


	private void scanMappers() throws ClassNotFoundException 
	{
		// use FastClasspathScanner to scan the dao package for @MappedClass
		ScanResult results = new FastClasspathScanner("dao").scan();	
		List<String> allResults = results.getNamesOfClassesWithAnnotation(MappedClass.class);
		
		for (String s: allResults) {
			Class c = Class.forName(s);
			MappedClass mc = (MappedClass) c.getAnnotation(MappedClass.class);
			Class mcClass = mc.clazz();
			// check if the clazz has the @Entity annotation
			if (mcClass.isAnnotationPresent(Entity.class)) {
				// map the clazz to the mapper class
				entityToMapperMap.put(mcClass, c);
			}
			else {
				// if not throw new RuntimeException("No @Entity")
				throw new RuntimeException("No @Entity");
			}
		}
		
		//Bonus 2
		for (Class c : entityToMapperMap.values()) {
			
			MappedClass mc = (MappedClass) c.getAnnotation(MappedClass.class);
			Class mcClass = mc.clazz();
			
			Method[] methods = c.getDeclaredMethods();
			Field[] fields = mcClass.getDeclaredFields();
			
			for(Method m : methods) {
				
				if (m.isAnnotationPresent(Select.class)) {
					
					Select select = m.getAnnotation(Select.class);
					String selectString = select.value();
					
					//For queries with @Param, check to see if the java type of the parameter is the same as the java type of the corresponding field in the entity
					//MAKE AN ASSUMPTION, the name used in the @Param is the actual column name in the SQL
					Parameter[] params = m.getParameters();
					if (params!=null) {
						
						for (Parameter param : params){
							Param p = param.getAnnotation(Param.class);
							if (p!=null) {
								
								//Check if the query string contains the placeholders for all the @Param names
								if(!select.value().contains(p.value())) {
									//If an error occurs throw a RuntimeException stating the error and stop the program
									throw new RuntimeException("Query string does not contain placeholder for "+p.value());
								}
								
								for (Field f : fields) {
									if (f.isAnnotationPresent(Column.class)) {
										Column column = f.getAnnotation(Column.class);
										
										//Finding the column name of the parameter
										String placeholder = ":"+p.value();
										int startIndex = selectString.indexOf(placeholder);
										String columnName = "";
										int count = 0;
										for (int i=startIndex; i>0; i--) {
											if (selectString.substring(i-1,i).equals("=")){
												count++;
											}
											else if (count>0 && !selectString.substring(i-1,i).equals(" ")) {
												columnName=selectString.substring(i-1,i)+columnName;
												count++;
											}
											else if (count>2 && selectString.substring(i-1,i).equals(" ")) {
												break;
											}
										}

										if (column.name().equals(columnName)) {
											if (param.getType()!=f.getType()) {
												//If an error occurs throw a RuntimeException stating the error and stop the program
												throw new RuntimeException("Java type of parameter does not match the one in entity");
											}
										}
									}
								}
								
							}
						}
					}
					
					//Check if every placeholder has a corresponding @Param
					ArrayList<String> placeholders = new ArrayList<>();
					for (int i=0; i<selectString.length(); i++) {
						String temp = "";
						int newIndex = 0;
						if (selectString.substring(i,i+1).equals(":")) {
							i++;
							while (!selectString.substring(i,i+1).equals(" ")) {
								temp+=selectString.substring(i,i+1);
								newIndex = i;
								if (i<selectString.length()-1) {
									i++;
								}
								else {
									break;
								}
							}
						}
						if (!temp.equals("")) {
							if (!temp.equals("table")) {
								placeholders.add(temp);
							}
							i=newIndex;
						}
					}
					for (int i=0; i<placeholders.size(); i++) {
						boolean found = false;
						if (params!=null) {
							for (Parameter param : params) {
								Param p = param.getAnnotation(Param.class);
								String val = p.value();
								if (val.contentEquals(placeholders.get(i))) {
									found = true;
								}
							}
						}
						if (!found) {
							//If an error occurs throw a RuntimeException stating the error and stop the program
							throw new RuntimeException ("Placeholder has no corresponsing @Param for :" + placeholders.get(i));
						}
						found = false;
					}
				}
			}	
		}
	}
	

	private void scanEntities() throws ClassNotFoundException 
	{
		// use FastClasspathScanner to scan the entity package for @Entity
		ScanResult results = new FastClasspathScanner("entity").scan();	
		List<String> allResults = results.getNamesOfClassesWithAnnotation(Entity.class);
		
		// go through each of the fields 
		for (String s : allResults) {
			Class c = Class.forName(s);
			Field[] fields = c.getDeclaredFields();
			// check if there is only 1 field with a Column id attribute
			int count = 0;
			for (Field f : fields)
			{
				if (f.isAnnotationPresent(Column.class))
				{
					Column column = f.getAnnotation(Column.class);
					if(column.id()==true) {
						count++;
					}
				}
			}
			if (count>1) {
				// if more than one field has id throw new RuntimeException("duplicate id=true")
				throw new RuntimeException("duplicate id=true");
			}
		}
	}
	
	
	public Object getMapper(Class clazz)
	{
		// create the proxy object for the mapper class supplied in clazz parameter
		// all proxies will use the supplied DaoInvocationHandler as the InvocationHandler
		Object object =  Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(), 					
				new Class[] { clazz }, 
				new DaoInvocationHandler()							
				);
		
		return object;
	}
	

	private void createTables()
	{
		// go through all the Mapper classes in the map
		for (Class c : entityToMapperMap.keySet()) {
			// create a proxy instance for each
			// all these proxies can be casted to BasicMapper
			Object object = getMapper(entityToMapperMap.get(c));
			BasicMapper basicMapper = (BasicMapper) object;
			// run the createTable() method on each of the proxies
			basicMapper.createTable();
		}
	}

	

	
	
}
