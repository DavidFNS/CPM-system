import { ICustomers } from 'app/entities/customers/customers.model';
import { IOrganization } from 'app/entities/organization/organization.model';

export interface IGroups {
  id: number;
  name?: string | null;
  grOwner?: Pick<ICustomers, 'id'> | null;
  organization?: Pick<IOrganization, 'id'> | null;
  users?: Pick<ICustomers, 'id'>[] | null;
}

export type NewGroups = Omit<IGroups, 'id'> & { id: null };
