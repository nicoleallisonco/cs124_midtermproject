//Added to test Bonus 1

package entity;

import annotations.Column;
import annotations.Entity;

@Entity
public class Test
{
	@Column(id=true)
	Integer id;


	public Integer getId() {
		return id;
	}


	public void setId(Integer id) {
		this.id = id;
	}


	@Column
	String name;


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	@Override
	public String toString() {
		return "Test [id=" + id + ", name=" + name + "]";
	}
	
	
}
